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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.impl.IdentityToken;
import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;
import ch.admin.vbs.cube.core.I18nBundleProvider;
import ch.admin.vbs.cube.core.IAuthModule;
import ch.admin.vbs.cube.core.IAuthModuleListener;
import ch.admin.vbs.cube.core.ILogin;
import ch.admin.vbs.cube.core.ILoginListener;
import ch.admin.vbs.cube.core.ILoginUI;
import ch.admin.vbs.cube.core.ILoginUI.LoginDialogType;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;

/**
 * Single user Login implementation (only one ID is active at a time).
 * 
 * This state machine aims to provide a consistent UI. User could interact by
 * removing or inserting his smart-card or y using the keyboard or the mouse.
 * The login/lock screen MUST ALWAYS react promptly to blank the screen when
 * needed.
 * 
 * SmartCard is slow, therefore we start opening token as soon as the token is
 * inserted, while we display the PIN dialog to user.
 * 
 */
public class LoginMachine implements ILogin, ITokenListener, IAuthModuleListener {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(LoginMachine.class);
	private static final int MAX_AUTH_ATTEMPTS = 3;
	// states
	private final StateLockedTokenIn stateLockedTokenIn = new StateLockedTokenIn();
	private final StateLockedTokenOut stateLockedTokenOut = new StateLockedTokenOut();
	private final StateWaitCreditential stateWaitCreditential = new StateWaitCreditential();
	private final StateLogedIn stateLogedIn = new StateLogedIn();
	private ResourceBundle bundle = I18nBundleProvider.getBundle();

	// transitions
	public enum StateTransition {
		TOKEN_REMOVED, TOKEN_INSERTED, AUTH_SUCCEED, AUTH_FAILED, LOGOUT;
	}

	private LinkedList<StateTransition> queue = new LinkedList<LoginMachine.StateTransition>();
	//
	private ArrayList<ILoginListener> listeners = new ArrayList<ILoginListener>(2);
	private IIdentityToken currentIdentity;
	// State
	private IState currentState;
	private AuthEventType failureReason;
	private int authAttempts = 0;
	// other beans
	private ITokenDevice tokenDevice;
	private IAuthModule authModule;
	private ILoginUI loginUI;

	@Override
	public void start() {
		// initial state
		setState(stateLockedTokenOut);
		// consume queue elements in an infinite loop.
		while (true) {
			// take the next transition to proceed
			StateTransition next = null;
			synchronized (queue) {
				next = queue.poll();
			}
			if (next == null) {
				// queue was empty. wait a little.
				synchronized (queue) {
					try {
						queue.wait(500);
					} catch (Exception e) {
						LOG.error("Failure", e);
					}
				}
			} else {
				// let the current state object proceed the given transition
				LOG.debug("Proceed transition [{}]. Queue remaining size [{}]", next, queue.size());
				currentState.transition(next);
			}
		}
	}

	@Override
	public void addListener(ILoginListener l) {
		listeners.add(l);
	}

	@Override
	public void discardAuthentication(IIdentityToken id) {
		if (currentIdentity == id) {
			queueTansistion(StateTransition.LOGOUT);
		}
	}

	@Override
	public void removeListener(ILoginListener l) {
		listeners.remove(l);
	}

	private void fireLockedEvent(IIdentityToken id) {
		for (ILoginListener l : listeners) {
			l.userLocked(id);
		}
	}

	private void fireAuthEvent(IIdentityToken id) {
		for (ILoginListener l : listeners) {
			l.userAuthentified(id);
		}
	}

	// ==================================================
	// React on event and queue appropriate transition. The
	// state machine run in its own thread, and it is important
	// not to block other threads.
	// ==================================================
	@Override
	public void notifyTokenEvent(TokenEvent event) {
		switch (event.getType()) {
		case TOKEN_INSERTED:
			queueTansistion(StateTransition.TOKEN_INSERTED);
			break;
		case TOKEN_REMOVED:
			queueTansistion(StateTransition.TOKEN_REMOVED);
			break;
		default:
			LOG.warn("Unsupported event [{}]", event.getType());
			break;
		}
	}

	@Override
	public void notifyAuthModuleEvent(AuthModuleEvent event) {
		switch (event.getType()) {
		case SUCCEED:
			currentIdentity = new IdentityToken(event.getKeystore(), event.getBuilder(), event.getPassword());
			queueTansistion(StateTransition.AUTH_SUCCEED);
			break;
		case FAILED_CARDTIMEOUT:
		case FAILED:
		case FAILED_CANCELED:
		case FAILED_WRONGPIN:
			failureReason = event.getType();
			queueTansistion(StateTransition.AUTH_FAILED);
			break;
		default:
			LOG.warn("Unsupported event [{}]", event.getType());
			break;
		}
	}

	private void queueTansistion(StateTransition transition) {
		synchronized (queue) {
			// enqueue transition
			queue.addLast(transition);
			LOG.debug("Queue transition [{}]. Queue size [{}]", transition, queue.size());
			// wake-up thread in while loop (see method 'start()')
			queue.notifyAll();
		}
	}

	// ==================================================
	// State machine methods
	// ==================================================
	private void setState(IState state) {
		LOG.debug("Change state [" + currentState + "]->[" + state + "]");
		currentState = state;
		state.proceed();
		state.refreshUI();
	}

	// ==================================================
	// States
	// ==================================================
	/**
	 * State interface
	 */
	public static interface IState {
		// This method will be called once the state has activated. The state
		// could therefore display a message or pop-up a dialog this way.
		public void refreshUI();

		public void proceed();

		// apply a transition to this state. The transition is the result of an
		// external event.The state will decide itself what will be the next
		// state of the machine.
		public void transition(StateTransition trs);
	}

	/**
	 * Default implementation of state interface
	 */
	public abstract class AbstractState implements IState {
		@Override
		public void transition(StateTransition trs) {
			// default implementation
			invalidTransition(trs);
		}

		protected void invalidTransition(StateTransition trs) {
			LOG.error("Invalid transition [{}] [{}]. Ignore.", trs, this);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

		@Override
		public void proceed() {
		}

		@Override
		public void refreshUI() {
		}
	}

	/** Initial state with token present. */
	public class StateLockedTokenIn extends AbstractState {
		@Override
		public void transition(StateTransition trs) {
			switch (trs) {
			case TOKEN_REMOVED:
				setState(stateLockedTokenOut);
				break;
			default:
				super.transition(trs);
				break;
			}
		}

		@Override
		public void refreshUI() {
			switch (failureReason) {
			case FAILED_WRONGPIN:
				loginUI.showDialog(bundle.getString("login.remove_smartcard_wrongpin"), LoginDialogType.NO_OPTION);
				break;
			case FAILED_CARDTIMEOUT:
				loginUI.showDialog(bundle.getString("login.remove_smartcard_carderror"), LoginDialogType.NO_OPTION);
				break;
			default:
				loginUI.showDialog(bundle.getString("login.remove_smartcard"), LoginDialogType.NO_OPTION);
				break;
			}
			failureReason = null;
		}
	}

	/** Initial state with token not present. */
	public class StateLockedTokenOut extends AbstractState {
		@Override
		public void transition(StateTransition transition) {
			switch (transition) {
			case TOKEN_INSERTED:
				setState(stateWaitCreditential);
				break;
			default:
				super.transition(transition);
				break;
			}
		}

		@Override
		public void refreshUI() {
			loginUI.showDialog(bundle.getString("login.insert_smartcard"), LoginDialogType.SHUTDOW_OPTION);
		}
	}

	/** Token is present and we ask user to enter its creditential (PIN). */
	public class StateWaitCreditential extends AbstractState {
		@Override
		public void transition(StateTransition trs) {
			switch (trs) {
			case TOKEN_REMOVED:
				authModule.abort();
				setState(stateLockedTokenOut);
				break;
			case AUTH_FAILED:
				authAttempts++;
				if (authAttempts >= MAX_AUTH_ATTEMPTS || failureReason == AuthEventType.FAILED_CARDTIMEOUT) {
					// force the user to remove smart-card
					setState(stateLockedTokenIn);
				} else {
					// allows user to re-submit a password
					setState(stateWaitCreditential);
				}
				break;
			case AUTH_SUCCEED:
				authAttempts = 0;
				setState(stateLogedIn);
				fireAuthEvent(currentIdentity);
				break;
			default:
				super.transition(trs);
				break;
			}
		}

		@Override
		public void proceed() {
			// start opening token while user enter its PIN
			authModule.openToken();
		}

		@Override
		public void refreshUI() {
			String message = null;
			if (authAttempts > 0) {
				message = bundle.getString("login.failed");
			}
			loginUI.showPinDialog(message, new CallbackPin(authModule));
		}
	}

	/** Token is present and we ask user to enter its creditential (PIN). */
	public class StateLogedIn extends AbstractState {
		@Override
		public void transition(StateTransition trs) {
			switch (trs) {
			case TOKEN_REMOVED: {
				IIdentityToken oldId = currentIdentity;
				authModule.abort();
				fireLockedEvent(oldId);
				currentIdentity = null;
				setState(stateLockedTokenOut);
				break;
			}
			case LOGOUT: {
				authModule.abort();
				// IIdentityToken oldId = currentIdentity;
				currentIdentity = null;
				// fireLogedOutEvent(oldId);
				setState(stateLockedTokenIn);
				break;
			}
			default:
				super.transition(trs);
				break;
			}
		}

		@Override
		public void refreshUI() {
			loginUI.closeDialog();
		}
	}

	/** Dependency injection */
	public void setup(IAuthModule authModule, ITokenDevice tokenDevice, ILoginUI loginUI) {
		this.authModule = authModule;
		this.tokenDevice = tokenDevice;
		this.tokenDevice = tokenDevice;
		this.loginUI = loginUI;
		// subscribe events out of them
		this.tokenDevice.addListener(this);
		this.authModule.addListener(this);
	}
}
