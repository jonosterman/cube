package ch.admin.vbs.cube3.core.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.impl.IdentityToken;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.impl.CaValidation;
import ch.admin.vbs.cube.core.impl.TokenEvent;
import ch.admin.vbs.cube3.core.ILogin;
import ch.admin.vbs.cube3.core.ILoginUI;
import ch.admin.vbs.cube3.core.ILoginUI.ILoginUIListener;
import ch.admin.vbs.cube3.core.mock.MockLoginUI;

public class StaticJKSLogin implements ILogin, ITokenListener, ILoginUIListener {
	// pkcs library
	private static final int MAX_FAILURE_CNT = 2;
	// listeners
	private ArrayList<ILoginListener> listeners = new ArrayList<ILogin.ILoginListener>(2);
	// DataStore stuff
	private Executor exec = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "StaticP12LoginTask");
			t.setDaemon(true);
			return t;
		}
	});
	private static final Logger LOG = LoggerFactory.getLogger(StaticJKSLogin.class);
	private SunPKCS11 provider;
	private Builder builder;
	private KeyStore keystore;
	private char[] password;
	private boolean passwordFlag;
	private CaValidation caValid = new CaValidation();
	private Lock lock = new ReentrantLock();
	private Task task;
	private int failureCountdown = MAX_FAILURE_CNT;
	private File jksFile;

	public StaticJKSLogin() {
		URL url = getClass().getResource("/cube-pki/client0.jks");
		jksFile = new File(url.getFile());
	}

	// ===============================================
	// Implements ITokenDevice
	// ===============================================
	@Override
	public void notifyTokenEvent(TokenEvent event) {
		lock.lock();
		try {
			// handle new request
			switch (event.getType()) {
			case TOKEN_INSERTED:
				// cancel current task
				if (task != null) {
					LOG.debug("Cancel old task before starting a new one.");
					task.cancel();
					task = null;
					password = null;
				}
				if (failureCountdown > 0) {
					// start new task
					LOG.debug("Start new task");
					task = new Task();
					exec.execute(task);
				} else {
					// will not try to open datastore because it may block the
					// smart-card definitevly.
					LOG.debug("Max failure count has been reached. Remove token.");
					fireEvent(Event.AUTHENTIFICATION_FAILURE_MAX, null);
				}
				break;
			case TOKEN_REMOVED:
				// cancel current task
				if (task != null) {
					LOG.debug("Cancel task because token has been removed");
					task.cancel();
					task = null;
				}
				failureCountdown = MAX_FAILURE_CNT;
				password = null;
				passwordFlag = false;
				break;
			}
		} finally {
			lock.unlock();
		}
	}

	public class Task implements Runnable, CallbackHandler {
		private boolean taskCancel;

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
				// initialize provider & builder
				builder = KeyStore.Builder.newInstance("JKS", null, jksFile, new KeyStore.CallbackHandlerProtection(this));
				if (taskCancel) {
					LOG.debug("Canceled.");
					return;
				}
				/*
				 * builder.getKeyStore will block until user give its password
				 * via method "handle(Callback[] callbacks)" and
				 * "handle(Callback[] callbacks)" will block until password is
				 * set via UI.
				 */
				keystore = builder.getKeyStore();
				// notify
				if (taskCancel) {
					LOG.debug("Canceled.");
					return;
				}
				failureCountdown = MAX_FAILURE_CNT;
				LOG.debug("User authentificated.");
				fireEvent(Event.USER_AUTHENTICATED, new IdentityToken(keystore, builder, password));
			} catch (Exception e) {
				if (taskCancel) {
					LOG.debug("Canceled [with exception]");
					return;
				}
				// failure countdown before requesting to remove token
				failureCountdown--;
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
				fireEvent(Event.AUTHENTIFICATION_FAILURE, null);
			}
			// remove itself from current running task
			task = null;
		}

		public void cancel() {
			taskCancel = true;
		}

		@Override
		/** Datastore call this method when it needs its password. */
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			// this.scAuthModule.enqueue(ScAuthStateTransition.PASSWORD_REQUEST);
			/* wait until user entered its password */
			while (!taskCancel && !passwordFlag) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					LOG.error("", e);
				}
			}
			if (taskCancel) {
				LOG.debug("callback will not return the password because task has been canceled");
				return;
			}
			LOG.debug("Provide password [{}].", new String(password));
			((PasswordCallback) callbacks[0]).setPassword(password);
		}
	}

	// ==================================================
	// IoC
	// ==================================================
	public void setup(ITokenDevice token, ILoginUI loginUI) {
		token.addListener(this);
		loginUI.addListener(this);
	}

	public void start() {
	}

	// ==================================================
	// Implements ILoginListener
	// ==================================================
	@Override
	public void setPassword(char[] passwd) {
		lock.lock();
		if (task == null) {
			LOG.debug("Ignore password because no task is set.");
		} else {
			password = passwd;
			passwordFlag = true;
		}
		lock.unlock();
	}

	@Override
	public void shutdown() {
	}

	// ==================================================
	// Implements ILogin
	// ==================================================
	@Override
	public void addListener(ILoginListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(ILoginListener l) {
		listeners.remove(l);
	}

	private void fireEvent(Event e, IIdentityToken id) {
		for (ILoginListener l : listeners) {
			l.processEvent(e, id);
		}
	}

	// ==================================================
	// Exception diagnose / handling
	// ==================================================
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
