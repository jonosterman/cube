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

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;
import ch.admin.vbs.cube.core.IAuthModule;
import ch.admin.vbs.cube.core.IAuthModuleListener;

/**
 * File-base authentication module
 * 
 * It is important not to block the state machine. So make your method return
 * ASAP (do crypto or UI stuff in another thread).
 */
public class P12AuthModule implements IAuthModule {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(P12AuthModule.class);
	private ArrayList<IAuthModuleListener> listeners = new ArrayList<IAuthModuleListener>(2);
	private Executor exec = Executors.newCachedThreadPool();
	private Object lock = new Object();
	private Builder builder;
	private KeyStore keystore;
	private AuthCallback currentCallback;
	private File p12File;

	public P12AuthModule(File p12File) {
		this.p12File = p12File;
	}

	@Override
	public void start() {
	}

	@Override
	public void abort() {
		synchronized (lock) {
			LOG.debug("Abort current authentification");
			if (currentCallback != null) {
				// disable callback
				currentCallback.active = false;
				// nullify all variables
				if (currentCallback.password != null) {
					Arrays.fill(currentCallback.password, '\0');
				}
				currentCallback.password = null;
				currentCallback = null;
				keystore = null;
				// unlock the thread that wait on password in
				// 'AuthCallback.handle()'
				lock.notifyAll();
			} else {
				LOG.debug("CurrentCallback is null. Ignore abort command.");
			}
		}
	}

	@Override
	public void addListener(IAuthModuleListener l) {
		listeners.add(l);
	}

	private void fireStateChanged(AuthModuleEvent event) {
		LOG.debug("Fire AuthModule event [{}].", event.getType());
		for (IAuthModuleListener l : listeners) {
			l.notifyAuthModuleEvent(event);
		}
	}

	@Override
	public void openToken() {
		synchronized (lock) {
			LOG.debug("Start authentification (open KeyStore in another thread).");
			// prepare callback object
			currentCallback = new AuthCallback();
			// execute builder thread
			exec.execute(new BuilderRunnable(currentCallback));
		}
	}

	@Override
	public void setPassword(char[] password) {
		synchronized (lock) {
			// set password if an authentication is currently running.
			if (currentCallback != null) {
				currentCallback.password = password;
				// unlock the thread that wait on password in
				// 'AuthCallback.handle()'
				lock.notifyAll();
			} else {
				LOG.debug("CurrentCallback is null. Ignore submited Password.");
			}
		}
	}

	/**
	 * We open the keystore in another thread, because it is slow, and we can
	 * ask the password to enter his password in the mean time.
	 */
	private class BuilderRunnable implements Runnable {
		private final AuthCallback callback;

		public BuilderRunnable(AuthCallback callback) {
			this.callback = callback;
		}

		@Override
		public void run() {
			// open keystore (blocking method). It will call the callback
			// method to get the user PIN and return only once the keystore
			// has been opened.
			try {
				// create builder
				LOG.debug("Open file [{}]", p12File.getAbsolutePath());
				builder = KeyStore.Builder.newInstance("PKCS12", null, p12File, new KeyStore.CallbackHandlerProtection(callback));
				// request keystore
				KeyStore keystoreTmp = builder.getKeyStore();// <- slow part
				// check that this builder is still 'active' before
				// updating keystore field in module.
				synchronized (lock) {
					if (callback == currentCallback && callback.active) {
						keystore = keystoreTmp;
						// notify succes
						fireStateChanged(new AuthModuleEvent(AuthEventType.SUCCEED, keystore, builder, callback.password));
					}
				}
			} catch (Exception e) {
				LOG.error("Failed to open keystore", e);
				synchronized (lock) {
					// check that this builder is still 'active' before
					// notifying failure.
					if (callback == currentCallback && callback.active) {
						LOG.debug("Failed to open KeyStore [{}]", e.getMessage());
						// notify failure
						fireStateChanged(new AuthModuleEvent(AuthEventType.FAILED, null, builder, null));
					}
				}
			} finally {
				// we can clear the password variable since the keystore is
				// opened now.
				callback.clearPassword();
			}
		}
	}

	private class AuthCallback implements CallbackHandler {
		public char[] password = null;
		private boolean active = true;

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			try {
				if (active) {
					for (Callback c : callbacks) {
						if (c instanceof PasswordCallback) {
							// if password has not been set yet. Wait for it.
							synchronized (lock) {
								if (!active)
									return;
								if (password == null) {
									try {
										LOG.debug("Wait for password..");
										lock.wait();
									} catch (InterruptedException e) {
										LOG.error("Failure", e);
									}
								} else {
									LOG.debug("Password available");
								}
								((PasswordCallback) c).setPassword(password);
								lock.notifyAll();
							}
						} else {
							LOG.error("Invalid callback [{}]", c);
						}
					}
				} else {
					// ignore
					LOG.debug("Skip callback. It has been canceled");
				}
			} catch (Exception e) {
				LOG.error("Callback failed", e);
			}
		}

		public void clearPassword() {
			if (password != null) {
				Arrays.fill(password, '\0');
			}
		}
	}
}
