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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.Security;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import sun.security.pkcs11.SunPKCS11;
import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;
import ch.admin.vbs.cube.core.impl.CaValidation;
import ch.admin.vbs.cube.core.impl.scauthmodule.AbstractState.ScAuthStateTransition;

class OpenKeyStoreTask implements Runnable, CallbackHandler {
	private final ScAuthModule scAuthModule;

	/**
	 * @param scAuthModule
	 */
	OpenKeyStoreTask(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	private SunPKCS11 provider;
	private Builder builder;
	private KeyStore keystore;
	private boolean keyStoreOpeningLock;
	private boolean killed;
	private char[] password;
	private int id = 0;
	private CaValidation caValid = new CaValidation();

	public void finalizeKeyStoreOpening() {
		keyStoreOpeningLock = false;
	}

	@Override
	public void run() {
		scAuthModule.setAbortReason(null);
		try {
			ScAuthModule.LOG.debug("Start auth [id:{}]", id);
			if (provider != null) {
				Security.removeProvider(provider.getName());
				provider = null;
			}
			StringBuilder buf = new StringBuilder();
			buf.append("library = ").append(this.scAuthModule.pkcs11LibraryPath).append("\nname = Cube\n");
			provider = new sun.security.pkcs11.SunPKCS11(new ByteArrayInputStream(buf.toString().getBytes()));
			Security.addProvider(provider);
			// create builder
			builder = KeyStore.Builder.newInstance("PKCS11", provider, new KeyStore.CallbackHandlerProtection(this));
			// request keystore
			ScAuthModule.LOG.debug("Open keystore...");
			// getKeyStore will block until user gave its password via
			// method "handle(Callback[] callbacks)" and
			// "handle(Callback[] callbacks)" will block until OpenKeyStoreState
			// will call 'finalizeKeyStoreOpening'
			ScAuthModule.LOG.debug("Opening KeyStore ..");
			keystore = builder.getKeyStore();
			// check certificates chain
			caValid.validate(keystore);
			// next transition / state
			this.scAuthModule.enqueue(ScAuthStateTransition.KEYSTORE_READY);
		} catch (Exception e) {
			if (this.scAuthModule.handlePinIncorrect(e)) {
				ScAuthModule.LOG.debug("Incorrect PIN (CKR_PIN_INCORRECT)");
				scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED_WRONGPIN, null, null, null));
			} else if (this.scAuthModule.handleCanceled(e)) {
				ScAuthModule.LOG.debug("PKCS11 login canceled");
				scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED, null, null, null));
			} else if (this.scAuthModule.handleUserNotLoggedIn(e)) {
				ScAuthModule.LOG.debug("User did not enter its password (CKR_USER_NOT_LOGGED_IN)");
				scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED_USERTIMEOUT, null, null, null));
			} else if (this.scAuthModule.handleNoSuchAlgo(e)) {
				ScAuthModule.LOG.debug("Unable to read smart-Card (NoSuchAlgorithmException)");
				scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED, null, null, null));
			} else if (this.scAuthModule.handleFunctionFailed(e)) {
				ScAuthModule.LOG.debug("Unable to use smart-Card (CKR_FUNCTION_FAILED)");
				scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED, null, null, null));
			} else {
				ScAuthModule.LOG.debug("Failed to open KeyStore", e);
				scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED, null, null, null));
			}
			this.scAuthModule.enqueue(ScAuthStateTransition.ABORT_AUTH);
		}
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		keyStoreOpeningLock = true;
		this.scAuthModule.enqueue(ScAuthStateTransition.PASSWORD_REQUEST);
		while (!killed && keyStoreOpeningLock) {
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				ScAuthModule.LOG.error("", e);
			}
		}
		((PasswordCallback) callbacks[0]).setPassword(password);
	}

	public KeyStore getKeyStore() {
		return keystore;
	}

	public Builder getBuilder() {
		return builder;
	}

	public char[] getPassword() {
		return password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}
}