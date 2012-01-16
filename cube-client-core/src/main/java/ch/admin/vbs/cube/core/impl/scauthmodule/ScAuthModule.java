/**
 * Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.admin.vbs.cube.core.impl.scauthmodule;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.pkcs11.wrapper.PKCS11Exception;
import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;
import ch.admin.vbs.cube.core.IAuthModule;
import ch.admin.vbs.cube.core.IAuthModuleListener;
import ch.admin.vbs.cube.core.impl.scauthmodule.AbstractState.ScAuthStateTransition;

/**
 * Smart-Card based IAuthModule.
 * 
 * It is important not to block the state machine. So make your method return
 * ASAP (do crypto or UI stuff in another thread).
 * 
 * <pre>
 * 
 *                 +----------------+
 *                 |      Idle      |<-----------------------.
 *                 +----------------|                        |
 *                          |    |                           | ABORT_AUTH
 *                          |    `--------------------------'|
 *                          |                                | 
 *                          | START_AUTH                     | 
 *                          |                                |
 *            +--------------------------+                   |
 *            | WaitKeyStoreAndPassword  |                   |
 *            +--------------------------+                   | 
 *                     |         |    |                      |
 *                     |         |    `---------------------'|
 *             ,-------'         `------.                    |
 *             |  PASSWORD_SUBMIT       | PASSWORD_REQUEST   |
 *             |                        |                    |
 *    +----------------+        +----------------+           |
 *    |  WaitKeyStore  |        |  WaitPassword  |----------'|
 *    +----------------+        +----------------+           |
 *            |     |                   |                    |
 * PASSWORD_REQUEST |                   | PASSWORD_SUMIT     |
 *            |     |                   |                    |
 *            |     `------------------ | ------------------'| 
 *            |                         |                    |
 *            `----------. ,------------'                    |
 *                       | |                                 |
 *            +--------------------------+                   |
 *            |      OpenKeyStore        |------------------'|
 *            +--------------------------+                   | 
 *                        |                                  |
 *                        | KEYSTORE_READY                   |
 *                        |                                  |
 *            +--------------------------+                   |
 *            |      OpenKeyReady        |-------------------'
 *            +--------------------------+
 * 
 * </pre>
 * 
 */
public class ScAuthModule implements IAuthModule, Runnable {
	static final long TIMEOUT_NO = 0;
	static final long TIMEOUT_USERINPUT = 120000;
	static final long TIMEOUT_KEYSTOREINIT = 10000;
	static final long TIMEOUT_KEYSTOREOPEN = 5000;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ScAuthModule.class);
	private static final String SC_PKCS11_LIBRARY_PROPERTY = "SCAdapter.pkcs11Library";
	String pkcs11LibraryPath;
	Executor exec = Executors.newCachedThreadPool();
	AbstractState activeState;
	private LinkedList<ScAuthStateTransition> transitions = new LinkedList<ScAuthStateTransition>();
	private ArrayList<IAuthModuleListener> listeners = new ArrayList<IAuthModuleListener>(2);
	boolean running;
	private OpenKeyStoreTask openKeyStoreTask;
	private StateWatchdog watchdog;
	private AuthModuleEvent abortReason = null;
	// pre-initialized states (grouped in class State for readability)
	States states = new States();

	class States {
		public IdleState idle = new IdleState(ScAuthModule.this);
		public KeyStoreReadyState keyStoreReady = new KeyStoreReadyState(ScAuthModule.this);
		public OpenKeyStoreState openKeyStore = new OpenKeyStoreState(ScAuthModule.this);
		public WaitKeystoreAndPasswordState waitKeyStoreAndPassword = new WaitKeystoreAndPasswordState(ScAuthModule.this);
		public WaitKeystoreState waitKeyStore = new WaitKeystoreState(ScAuthModule.this);
		public WaitPasswordState waitPasswordState = new WaitPasswordState(ScAuthModule.this);
	}

	// =============================================
	// IAuthModule
	// =============================================
	@Override
	public void abort() {
		enqueue(ScAuthStateTransition.ABORT_AUTH);
	}

	@Override
	public void openToken() {
		enqueue(ScAuthStateTransition.START_AUTH);
	}

	@Override
	public void setPassword(char[] password) {
		/*
		 * openKeyStoreTask SHOULD not be null since we set it in start(). But
		 * since a race condition may occur, we have to test it.
		 */
		if (openKeyStoreTask == null) {
			LOG.warn("Race condition: got passowrd when openKeyStoreTask is still not ready. ignore event");
		} else {
			LOG.debug("set the passord on the OpenKeyStoreTask that run in background");
			openKeyStoreTask.setPassword(password);
			enqueue(ScAuthStateTransition.PASSWORD_SUBMIT);
		}
	}

	@Override
	public void start() {
		running = true;
		// get PKCS11 library path from configuration file
		pkcs11LibraryPath = CubeClientCoreProperties.getProperty(SC_PKCS11_LIBRARY_PROPERTY);
		// Create and start state's watchdog
		watchdog = new StateWatchdog(this);
		exec.execute(watchdog);
		// ...
		openKeyStoreTask = new OpenKeyStoreTask(this);
		// Start State Machine's thread
		exec.execute(this);
	}

	@Override
	public void run() {
		// initial ScAuthModule state
		activeState = new IdleState(this);
		activeState.proceed();
		// loop pretty much forever
		while (running) {
			// consume enqueued transitions
			while (transitions.size() > 0) {
				// next transition
				synchronized (transitions) {
					ScAuthStateTransition tr = transitions.removeFirst();
					// reference to current state
					AbstractState lastState = activeState;
					// process transition and retrieve the new state
					activeState = activeState.transition(tr);
					LOG.debug("Apply transition [{}] on state [{}] => [" + activeState + "]", tr, lastState);
					// proceed new state logic
				}
				activeState.proceed();
			}
			// wait next transition
			synchronized (transitions) {
				while (transitions.size() == 0) {
					try {
						transitions.wait(30000);
					} catch (InterruptedException e) {
						LOG.error("Transition wait loop failure", e);
					}
				}
			}
		}
	}

	void enqueue(ScAuthStateTransition t) {
		synchronized (transitions) {
			transitions.add(t);
			transitions.notifyAll();
		}
	}

	@Override
	public void addListener(IAuthModuleListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	void fireStateChanged(final AuthModuleEvent event) {
		/*
		 * Notify listeners in another thread, because we MUST not block the
		 * state machine.
		 */
		exec.execute(new Runnable() {
			@Override
			public void run() {
				LOG.debug("Fire AuthModule event [{}].", event.getType());
				for (IAuthModuleListener l : listeners) {
					l.notifyAuthModuleEvent(event);
				}
			}
		});
	}

	/** exception handling */
	boolean handleCanceled(Exception e) {
		return e instanceof RuntimeException && "cancel".equals(e.getMessage());
	}

	/** exception handling */
	boolean handlePinIncorrect(Exception e) {
		// search specific exception in call stack
		Throwable x = e;
		while (x != null) {
			if (x instanceof PKCS11Exception && "CKR_PIN_INCORRECT".equals(x.getMessage())) {
				return true;
			}
			x = x.getCause();
		}
		return false;
	}

	/** exception handling */
	boolean handleNoSuchAlgo(Exception e) {
		// search specific exception in call stack
		Throwable x = e;
		while (x != null) {
			if (x instanceof NoSuchAlgorithmException) {
				return true;
			}
			x = x.getCause();
		}
		return false;
	}

	/** exception handling */
	boolean handleUserNotLoggedIn(Exception e) {
		// search specific exception in call stack
		Throwable x = e;
		while (x != null) {
			if (x instanceof PKCS11Exception && "CKR_USER_NOT_LOGGED_IN".equals(x.getMessage())) {
				return true;
			}
			x = x.getCause();
		}
		return false;
	}

	/** exception handling */
	boolean handleFunctionFailed(Exception e) {
		// search specific exception in call stack
		Throwable x = e;
		while (x != null) {
			if (x instanceof PKCS11Exception && "CKR_FUNCTION_FAILED".equals(x.getMessage())) {
				return true;
			}
			x = x.getCause();
		}
		return false;
	}

	public void setAbortReason(AuthModuleEvent abortReason) {
		this.abortReason = abortReason;
	}

	public AuthModuleEvent getAbortReason() {
		return abortReason;
	}

	public OpenKeyStoreTask getOpenKeyStoreTask() {
		return openKeyStoreTask;
	}
}
