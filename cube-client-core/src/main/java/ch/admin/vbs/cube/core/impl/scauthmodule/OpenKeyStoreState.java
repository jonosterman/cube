
package ch.admin.vbs.cube.core.impl.scauthmodule;

/**
 * Once KeyStore is ready (has asked for user password) and user effectively
 * entered its password, ScAuthModule jump in this state to trigger
 * OpenKeyStoreTask to finalize KeyStore opening and wait to it to append.
 */
class OpenKeyStoreState extends AbstractState {
	private final ScAuthModule scAuthModule;

	/**
	 * @param scAuthModule
	 */
	public OpenKeyStoreState(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	@Override
	public void proceed() {
		// set a timeout since sometimes the smartcard driver hangs forever
		resetTimeout(ScAuthModule.TIMEOUT_KEYSTOREOPEN);
		// trigger OpenKeyStoreTask to finalize KeyStoreOpenning
		if (scAuthModule.getOpenKeyStoreTask() != null) {
			scAuthModule.getOpenKeyStoreTask().finalizeKeyStoreOpening();
		}
	}

	@Override
	public AbstractState transition(ScAuthStateTransition trs) {
		switch (trs) {
		case KEYSTORE_READY:
			// OpenKeyStoreTask says us that KeyStore is ready -> jump to KeyStoreReadyState
			return scAuthModule.getStateInstance(KeyStoreReadyState.class);
		case ABORT_AUTH:
			// return to IdleSate if something goes wrong
			return scAuthModule.getStateInstance(IdleState.class);

		default:
			return super.transition(trs);
		}
	}
}