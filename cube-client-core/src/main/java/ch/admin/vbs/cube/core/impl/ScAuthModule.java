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

package ch.admin.vbs.cube.core.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.pkcs11.SunPKCS11;
import sun.security.pkcs11.wrapper.PKCS11Exception;
import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;
import ch.admin.vbs.cube.core.IAuthModule;
import ch.admin.vbs.cube.core.IAuthModuleListener;

/**
 * It is important not to block the state machine. So make your method return
 * ASAP (do crypto or UI stuff in another thread).
 */
public class ScAuthModule implements IAuthModule, Runnable {
	private static final long TIMEOUT_NO = 0;
	private static final long TIMEOUT_USERINPUT = 60000;
	private static final long TIMEOUT_KEYSTOREINIT = 10000;
	private static final long TIMEOUT_KEYSTOREOPEN = 5000;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ScAuthModule.class);
	private static final String SC_PKCS11_LIBRARY_PROPERTY = "SCAdapter.pkcs11Library";
	private String pkcs11LibraryPath;
	private Executor exec = Executors.newCachedThreadPool();
	private AbstractState activeState;
	private LinkedList<StateTransition> transitions = new LinkedList<ScAuthModule.StateTransition>();
	private ArrayList<IAuthModuleListener> listeners = new ArrayList<IAuthModuleListener>(2);
	private boolean running;
	private OpenKeyStoreTask openKeyStoreTask;
	private CaValidation caValid;
	private StateWatchdog watchdog;

	// =============================================
	// IAuthModule
	// =============================================
	@Override
	public void abort() {
		enqueue(StateTransition.ABORT_AUTH);
	}

	@Override
	public void addListener(IAuthModuleListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	@Override
	public void openToken() {
		enqueue(StateTransition.START_AUTH);
	}

	@Override
	public void setPassword(char[] password) {
		if (openKeyStoreTask != null) {
			openKeyStoreTask.password = password;
		}
		enqueue(StateTransition.PASSWORD_SUBMIT);
	}

	@Override
	public void start() {
		running = true;
		// get PKCS11 library path from configuration file
		pkcs11LibraryPath = CubeClientCoreProperties.getProperty(SC_PKCS11_LIBRARY_PROPERTY);
		// create certificate-chain validator
		caValid = new CaValidation();
		// Create and start state's watchdog
		watchdog = new StateWatchdog();
		exec.execute(watchdog);
		// ...
		openKeyStoreTask = new OpenKeyStoreTask();
		// Start State Machine's thread
		exec.execute(this);
	}

	@Override
	public void run() {
		// initial ScAuthModule state
		activeState = new IdleState();
		activeState.proceed();
		// loop
		while (running) {
			StateTransition tr = null;
			synchronized (transitions) {
				if (transitions.size() > 0) {
					tr = transitions.removeFirst();
					AbstractState ostate = activeState;
					activeState = activeState.transition(tr);
					LOG.debug("Apply transition [{}] on state [{}] => [" + activeState + "]", tr, ostate);
					activeState.proceed();
				}
			}
			if (tr == null) {
				synchronized (transitions) {
					try {
						transitions.wait(1000);
					} catch (InterruptedException e) {
						LOG.error("", e);
					}
				}
			}
		}
	}

	// =============================================
	// States
	// =============================================
	private AbstractState idleState = new IdleState();
	private AbstractState waitKSPwdState = new WaitKeystoreAndPasswordState();
	private AbstractState waitKSState = new WaitKeystoreState();
	private AbstractState waitPwdState = new WaitPasswordState();
	private AbstractState openKSState = new OpenKeyStoreState();
	private AbstractState keyStoreReadyState = new KeyStoreReadyState();

	private void enqueue(StateTransition t) {
		synchronized (transitions) {
			transitions.add(t);
			transitions.notifyAll();
		}
	}

	// transitions
	public enum StateTransition {
		TIMEOUT, START_AUTH, ABORT_AUTH, PASSWORD_SUBMIT, PASSWORD_REQUEST, KEYSTORE_READY;
	}

	private abstract class AbstractState {
		private long deadline = 0;

		public AbstractState transition(StateTransition trs) {
			// default implementation
			invalidTransition(trs);
			return this;
		}

		protected void invalidTransition(StateTransition trs) {
			LOG.error("Invalid transition [{}] [{}]. Ignore.", trs, this);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

		public void proceed() {
		}

		protected void setTimeout(long timeout) {
			deadline = timeout == 0 ? 0 : System.currentTimeMillis() + timeout;
		}
	}

	private class IdleState extends AbstractState {
		@Override
		public void proceed() {
			setTimeout(TIMEOUT_NO);
			if (openKeyStoreTask != null) {
				AuthModuleEvent event = openKeyStoreTask.abortReason;
				if (event != null) {
					fireStateChanged(event);
				}
			}
		}

		@Override
		public AbstractState transition(StateTransition trs) {
			switch (trs) {
			case START_AUTH:
				return waitKSPwdState;
			case ABORT_AUTH:
				return idleState;
			default:
				return super.transition(trs);
			}
		}
	}

	private class WaitKeystoreAndPasswordState extends AbstractState {
		@Override
		public void proceed() {
			setTimeout(TIMEOUT_KEYSTOREINIT);
			exec.execute(openKeyStoreTask);
		}

		@Override
		public AbstractState transition(StateTransition trs) {
			switch (trs) {
			case PASSWORD_SUBMIT:
				return waitKSState;
			case PASSWORD_REQUEST:
				return waitPwdState;
			case ABORT_AUTH:
				return idleState;
			default:
				return super.transition(trs);
			}
		}
	}

	private class WaitKeystoreState extends AbstractState {
		@Override
		public void proceed() {
			setTimeout(TIMEOUT_KEYSTOREINIT);
		}

		@Override
		public AbstractState transition(StateTransition trs) {
			switch (trs) {
			case PASSWORD_REQUEST:
				return openKSState;
			case ABORT_AUTH:
				return idleState;
			default:
				return super.transition(trs);
			}
		}
	}

	private class WaitPasswordState extends AbstractState {
		@Override
		public void proceed() {
			setTimeout(TIMEOUT_USERINPUT);
		}

		@Override
		public AbstractState transition(StateTransition trs) {
			switch (trs) {
			case PASSWORD_SUBMIT:
				return openKSState;
			case ABORT_AUTH:
				return idleState;
			default:
				return super.transition(trs);
			}
		}
	}

	private class OpenKeyStoreState extends AbstractState {
		@Override
		public void proceed() {
			setTimeout(TIMEOUT_KEYSTOREOPEN);
			if (openKeyStoreTask != null) {
				openKeyStoreTask.waitPassword = false;
			}
		}

		@Override
		public AbstractState transition(StateTransition trs) {
			switch (trs) {
			case KEYSTORE_READY:
				return keyStoreReadyState;
			case ABORT_AUTH:
				return idleState;
			default:
				return super.transition(trs);
			}
		}
	}

	private class KeyStoreReadyState extends AbstractState {
		@Override
		public void proceed() {
			setTimeout(TIMEOUT_NO);
			fireStateChanged(new AuthModuleEvent(AuthEventType.SUCCEED, openKeyStoreTask.keystore, openKeyStoreTask.builder, openKeyStoreTask.password));
		}

		@Override
		public AbstractState transition(StateTransition trs) {
			switch (trs) {
			case ABORT_AUTH:
				return idleState;
			default:
				return super.transition(trs);
			}
		}
	}

	// =============================================
	// KeysStore open task
	// =============================================
	private class OpenKeyStoreTask implements Runnable, CallbackHandler {
		private SunPKCS11 provider;
		private Builder builder;
		private KeyStore keystore;
		private boolean waitPassword;
		private boolean killed;
		private char[] password;
		private int id = 0;
		private AuthModuleEvent abortReason = null;

		@Override
		public void run() {
			abortReason = null;
			try {
				LOG.debug("Start auth [id:{}]", id);
				if (provider != null) {
					Security.removeProvider(provider.getName());
					provider = null;
				}
				StringBuilder buf = new StringBuilder();
				buf.append("library = ").append(pkcs11LibraryPath).append("\nname = Cube\n");
				provider = new sun.security.pkcs11.SunPKCS11(new ByteArrayInputStream(buf.toString().getBytes()));
				Security.addProvider(provider);
				// create builder
				builder = KeyStore.Builder.newInstance("PKCS11", provider, new KeyStore.CallbackHandlerProtection(this));
				// request keystore
				LOG.debug("Open keystore...");
				// getKeyStore will block until user gave its password via
				// method "handle(Callback[] callbacks)"
				LOG.debug("Opening KeyStore ..");
				keystore = builder.getKeyStore();
				enqueue(StateTransition.KEYSTORE_READY);
			} catch (Exception e) {
				if (handlePinIncorrect(e)) {
					LOG.debug("Incorrect PIN (CKR_PIN_INCORRECT)");
					abortReason = new AuthModuleEvent(AuthEventType.FAILED_WRONGPIN, null, null, null);
				} else if (handleCanceled(e)) {
					LOG.debug("PKCS11 login canceled");
					abortReason = new AuthModuleEvent(AuthEventType.FAILED, null, null, null);
				} else if (handleUSerNotLoggedIn(e)) {
					LOG.debug("User did not enter its password (CKR_USER_NOT_LOGGED_IN)");
					abortReason = new AuthModuleEvent(AuthEventType.FAILED_USERTIMEOUT, null, null, null);
				} else if (handleNoSuchAlgo(e)) {
					LOG.debug("Unable to read smart-Card (NoSuchAlgorithmException)" );
					abortReason = new AuthModuleEvent(AuthEventType.FAILED, null, null, null);
				} else if (handleFunctionFailed(e)) {
					LOG.debug("Unable to use smart-Card (CKR_FUNCTION_FAILED)");
					abortReason = new AuthModuleEvent(AuthEventType.FAILED, null, null, null);
				} else {
					/*
					 * a 'PKCS11Exception: CKR_FUNCTION_FAILED' error could be
					 * raised if user removed his smart-card during login. Log
					 * it as 'trace'.
					 */
					LOG.debug("Failed to open KeyStore", e);
					abortReason = new AuthModuleEvent(AuthEventType.FAILED, null, null, null);
				}
				enqueue(StateTransition.ABORT_AUTH);
			}
		}

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			waitPassword = true;
			enqueue(StateTransition.PASSWORD_REQUEST);
			while (!killed && waitPassword) {
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					LOG.error("", e);
				}
			}
			((PasswordCallback) callbacks[0]).setPassword(password);
		}
	}

	private void fireStateChanged(final AuthModuleEvent event) {
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

	private class StateWatchdog implements Runnable {
		private AbstractState wstate;

		@Override
		public void run() {
			while (running) {
				// get a reference on active state
				AbstractState tstate = activeState;
				// test if deadline is defined and expired
				if (tstate != null && tstate.deadline != 0 && tstate.deadline < System.currentTimeMillis()) {
					LOG.debug("Abort state [{}] due to timeout", tstate);
					tstate.deadline = 0; // reset deadline to
					if (tstate == waitPwdState) {
					openKeyStoreTask.abortReason = new AuthModuleEvent(AuthEventType.FAILED_USERTIMEOUT, null, null, null);
					} else {
						openKeyStoreTask.abortReason = new AuthModuleEvent(AuthEventType.FAILED_CARDTIMEOUT, null, null, null);
					}
					enqueue(StateTransition.ABORT_AUTH);
				}
				// log..
				if (tstate != null && tstate.deadline > 0) {
					LOG.debug("Monitor state [{}] : remaining {} ms", tstate, tstate.deadline - System.currentTimeMillis());
				}
				// sleep
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOG.error("", e);
				}
			}
		}
	}

	private boolean handleCanceled(Exception e) {
		return e instanceof RuntimeException && "cancel".equals(e.getMessage());
	}

	private boolean handlePinIncorrect(Exception e) {
		Throwable x = e;
		while (x != null) {
			if (x instanceof PKCS11Exception && "CKR_PIN_INCORRECT".equals(x.getMessage())) {
				return true;
			}
			x = x.getCause();
		}
		return false;
	}

	private boolean handleNoSuchAlgo(Exception e) {
		Throwable x = e;
		while (x != null) {
			if (x instanceof NoSuchAlgorithmException) {
				return true;
			}
			x = x.getCause();
		}
		return false;
	}

	private boolean handleUSerNotLoggedIn(Exception e) {
		Throwable x = e;
		while (x != null) {
			if (x instanceof PKCS11Exception && "CKR_USER_NOT_LOGGED_IN".equals(x.getMessage())) {
				return true;
			}
			x = x.getCause();
		}
		return false;
	}	private boolean handleFunctionFailed(Exception e) {
		Throwable x = e;
		while (x != null) {
			if (x instanceof PKCS11Exception && "CKR_FUNCTION_FAILED".equals(x.getMessage())) {
				return true;
			}
			x = x.getCause();
		}
		return false;
	}
}
