/**
 * Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
import java.security.Security;
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

import sun.security.pkcs11.SunPKCS11;
import sun.security.pkcs11.wrapper.PKCS11Exception;
import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;
import ch.admin.vbs.cube.core.IAuthModule;
import ch.admin.vbs.cube.core.IAuthModuleListener;

/**
 * 
 * 
 * It is important not to block the state machine. So make your method return
 * ASAP (do crypto or UI stuff in another thread).
 */
public class ScAuthModule implements IAuthModule {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ScAuthModule.class);
	private static final String SC_PKCS11_LIBRARY_PROPERTY = "SCAdapter.pkcs11Library";
	private ArrayList<IAuthModuleListener> listeners = new ArrayList<IAuthModuleListener>(2);
	private Executor exec = Executors.newCachedThreadPool();
	private Object lock = new Object();
	private Builder builder;
	private AuthCallback currentCallback;
	private String pkcs11LibraryPath;
	private CaValidation caValid;
	private static SunPKCS11 provider;

	@Override
	public void start() {
		pkcs11LibraryPath = CubeClientCoreProperties.getProperty(SC_PKCS11_LIBRARY_PROPERTY);
		caValid = new CaValidation();
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
		private KeyStore keystore;

		public BuilderRunnable(AuthCallback callback) {
			this.callback = callback;
		}

		@Override
		public void run() {
			// open keystore. It will block until the user entered its PIN (via
			// Callback object). The call to 'builder.getKeyStore()' is pretty
			// slow and there is a 1 second delay before it triggers the
			// callback that ask for password. In order to make the login more
			// responsive, we run this part in a separated thread while we
			// already ask the user to enter its PIN.
			try {
				// initialize PKCS11 provider from Java API. We MUST remove the
				// provider each time or it will bug when switching to another
				// smartcard (buggy provider?).
				LOG.debug("Initialize SunPKCS11");
				if (provider != null) {
					Security.removeProvider(provider.getName());
				}
				StringBuilder buf = new StringBuilder();
				buf.append("library = ").append(pkcs11LibraryPath).append("\nname = Cube\n");
				provider = new sun.security.pkcs11.SunPKCS11(new ByteArrayInputStream(buf.toString().getBytes()));
				Security.addProvider(provider);
				// create builder
				builder = KeyStore.Builder.newInstance("PKCS11", provider, new KeyStore.CallbackHandlerProtection(callback));
				// request keystore
				LOG.debug("Request keystore (slow)");
				keystore = builder.getKeyStore();// <- slow part
				LOG.debug("Got keystore");
				// validate keystore with root CA
				if (!caValid.validate(keystore)) {
					throw new CubeException("Certificates on smart-cards are invalid.");
				}
				// check that this builder is still 'active' before
				// updating keystore field in module.
				synchronized (lock) {
					if (callback == currentCallback && callback.active) {
						LOG.debug("KeyStore opened and set as active for this module [{}]", keystore.aliases());
						// notify success
						fireStateChanged(new AuthModuleEvent(AuthEventType.SUCCEED, keystore, builder, callback.password));
					}
				}
			} catch (Exception e) {
				synchronized (lock) {
					// check that this builder is still 'active' before
					// notifying failure.
					if (callback == currentCallback && callback.active) {
						if (handlePinIncorrect(e)) {
							LOG.debug("Incorrect PIN");
						} else {
							// a 'PKCS11Exception: CKR_FUNCTION_FAILED' error
							// could be rised if
							// user removed his smart-card during login. Log it
							// as debug.
							LOG.debug("Failed to open KeyStore", e);
						}
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
	}

	private class AuthCallback implements CallbackHandler {
		public char[] password;
		private boolean active = true;

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			if (active) {
				for (Callback c : callbacks) {
					if (c instanceof PasswordCallback) {
						PasswordCallback pwdCbk = (PasswordCallback) c;
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
								LOG.debug("Password already ready");
							}
							pwdCbk.setPassword(password);
							lock.notifyAll();
						}
					}
				}
			} else {
				// ignore
				LOG.debug("Skip callback. It has been canceled");
			}
		}

		public void clearPassword() {
			if (password != null) {
				Arrays.fill(password, '\0');
			}
		}
	}
}
