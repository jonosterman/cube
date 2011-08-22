
package ch.admin.vbs.cube.core.impl.scauthmodule;

import ch.admin.vbs.cube.core.AuthModuleEvent;

/**
 * Initial state. It will transit to "wait password and keystore" state as soon
 * as it receive a START_AUTH event (via IAuthModule.openToken()).
 */
class IdleState extends AbstractState {
	private final ScAuthModule scAuthModule;

	/**
	 * @param scAuthModule
	 */
	public IdleState(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	@Override
	public void proceed() {
		// We may rest in this state indefinitely
		resetTimeout(ScAuthModule.TIMEOUT_NO);
		// check if 'abortReason' is set and display a dialog accordingly.
		AuthModuleEvent event = scAuthModule.getAbortReason();
		if (event != null) {
			this.scAuthModule.fireStateChanged(event);
		}
	}

	@Override
	public AbstractState transition(ScAuthStateTransition trs) {
		switch (trs) {
		case START_AUTH:
			return scAuthModule.getStateInstance(WaitKeystoreAndPasswordState.class);
		case ABORT_AUTH:
			return scAuthModule.getStateInstance(IdleState.class);
		default:
			return super.transition(trs);
		}
	}
}