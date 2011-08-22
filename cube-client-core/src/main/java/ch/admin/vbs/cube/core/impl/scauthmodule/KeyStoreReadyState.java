
package ch.admin.vbs.cube.core.impl.scauthmodule;

import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;

/**
 * Final state. KeyStore has been opened successfully.
 */
class KeyStoreReadyState extends AbstractState {
	private final ScAuthModule scAuthModule;

	public KeyStoreReadyState(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	@Override
	public void proceed() {
		// we may stay in this state forever
		resetTimeout(ScAuthModule.TIMEOUT_NO);
		// fire event 'authentication succeed' to update UI
		scAuthModule.fireStateChanged(new AuthModuleEvent(AuthEventType.SUCCEED, scAuthModule.getOpenKeyStoreTask().getKeyStore(),
				scAuthModule.getOpenKeyStoreTask().getBuilder(), scAuthModule.getOpenKeyStoreTask().getPassword()));
	}

	@Override
	public AbstractState transition(ScAuthStateTransition trs) {
		switch (trs) {
		case ABORT_AUTH:
			// return to IdleSate if something goes wrong
			return scAuthModule.getStateInstance(IdleState.class);
		default:
			return super.transition(trs);
		}
	}
}