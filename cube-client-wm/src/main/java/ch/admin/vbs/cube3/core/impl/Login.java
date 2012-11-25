package ch.admin.vbs.cube3.core.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.pkcs11.SunPKCS11;
import sun.security.pkcs11.wrapper.PKCS11Exception;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.impl.CaValidation;
import ch.admin.vbs.cube.core.impl.TokenEvent;
import ch.admin.vbs.cube.core.impl.scauthmodule.OpenKeyStoreTask;
import ch.admin.vbs.cube3.core.ILogin;

public class Login implements ILogin, ITokenListener {
	// pkcs library
	private static final String SC_PKCS11_LIBRARY_PROPERTY = "SCAdapter.pkcs11Library";
	private String pkcs11LibraryPath;
	// listeners
	private ArrayList<LoginListener> listeners = new ArrayList<ILogin.LoginListener>(2);
	// DataStore stuff
	private Executor exec = Executors.newCachedThreadPool();
	private static final Logger LOG = LoggerFactory.getLogger(OpenKeyStoreTask.class);
	private SunPKCS11 provider;
	private Builder builder;
	private KeyStore keystore;
	private boolean killed;
	private char[] password;
	private CaValidation caValid = new CaValidation();
	private Lock lock = new ReentrantLock();
	private Task task;

	public Login() {
		// get PKCS11 library path from configuration file
		pkcs11LibraryPath = CubeClientCoreProperties.getProperty(SC_PKCS11_LIBRARY_PROPERTY);
	}

	@Override
	public void notifyTokenEvent(TokenEvent event) {
		lock.lock();
		try {
			// handle new request
			switch (event.getType()) {
			case TOKEN_INSERTED:
				// cancel old task
				if (task != null) {
					task.cancel();
					password = null;
				}
				task = new Task();
				exec.execute(task);
				break;
			case TOKEN_REMOVED:
				if (task != null) {
					task.cancel();
					password = null;
				}
				break;
			}
		} finally {
			lock.unlock();
		}
	}

	public class Task implements Runnable, CallbackHandler {
		private boolean canceled;
		private boolean keyStoreOpeningLock;

		@Override
		public void run() {
			try {
				/*
				 * cleanup already register provider seems needed in order to
				 * reset smart-card driver.
				 */
				if (provider != null) {
					Security.removeProvider(provider.getName());
					provider = null;
				}
				// initialize provider
				StringBuilder buf = new StringBuilder();
				buf.append("library = ").append(pkcs11LibraryPath).append("\nname = Cube\n");
				LOG.debug("## PKCS11 Config ##########\n" + buf.toString() + "\n############");
				provider = new sun.security.pkcs11.SunPKCS11(new ByteArrayInputStream(buf.toString().getBytes()));
				Security.addProvider(provider);
				// create builder
				builder = KeyStore.Builder.newInstance("PKCS11", provider, new KeyStore.CallbackHandlerProtection(this));
				/*
				 * request keystore getKeyStore will block until user gave its
				 * password via method "handle(Callback[] callbacks)" and
				 * "handle(Callback[] callbacks)" will block until
				 * OpenKeyStoreState will call 'finalizeKeyStoreOpening'
				 */
				if (canceled) {
					LOG.debug("Canceled.");
					return;
				}
				keystore = builder.getKeyStore();
				// check certificates chain
				caValid.validate(keystore);
			} catch (Exception e) {
				if (canceled) {
					LOG.debug("Canceled [in exception]");
					return;
				}
				/*
				 * try to guess error cause in order to give the user a better
				 * feedback.
				 */
				if (handlePinIncorrect(e)) {
					LOG.debug("OpenKeyStoreTask failed: Incorrect PIN (CKR_PIN_INCORRECT)");
					// scAuthModule.setAbortReason(new
					// AuthModuleEvent(AuthEventType.FAILED_WRONGPIN, null,
					// null,
					// null));
				} else if (handleCanceled(e)) {
					LOG.debug("OpenKeyStoreTask failed: PKCS11 login canceled");
					// scAuthModule.setAbortReason(new
					// AuthModuleEvent(AuthEventType.FAILED, null, null, null));
				} else if (handleUserNotLoggedIn(e)) {
					LOG.debug("OpenKeyStoreTask failed: User did not enter its password (CKR_USER_NOT_LOGGED_IN)");
					// scAuthModule.setAbortReason(new
					// AuthModuleEvent(AuthEventType.FAILED_USERTIMEOUT, null,
					// null,
					// null));
				} else if (handleNoSuchAlgo(e)) {
					LOG.debug("OpenKeyStoreTask failed: Unable to read smart-Card (NoSuchAlgorithmException)");
					// scAuthModule.setAbortReason(new
					// AuthModuleEvent(AuthEventType.FAILED, null, null, null));
				} else if (handleFunctionFailed(e)) {
					LOG.debug("OpenKeyStoreTask failed: Unable to use smart-Card (CKR_FUNCTION_FAILED)");
					// scAuthModule.setAbortReason(new
					// AuthModuleEvent(AuthEventType.FAILED, null, null, null));
				} else {
					LOG.debug("OpenKeyStoreTask failed: general failure", e);
					// scAuthModule.setAbortReason(new
					// AuthModuleEvent(AuthEventType.FAILED, null, null, null));
				}
			}
		}

		public void cancel() {
			canceled = true;
		}

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			keyStoreOpeningLock = true;
			// this.scAuthModule.enqueue(ScAuthStateTransition.PASSWORD_REQUEST);
			/* wait until user entered its password */
			while (!killed && keyStoreOpeningLock) {
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					LOG.error("", e);
				}
			}
			((PasswordCallback) callbacks[0]).setPassword(password);
		}
	}

	public void setup(ITokenDevice token) {
		token.addListener(this);
	}

	public void start() {
	}

	@Override
	public void addListener(LoginListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(LoginListener l) {
		listeners.remove(l);
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
}
