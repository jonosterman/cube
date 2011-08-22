package ch.admin.vbs.cube.core.impl.scauthmodule;


class WaitKeystoreState extends AbstractState {
	/**
	 * 
	 */
	private final ScAuthModule scAuthModule;

	/**
	 * @param scAuthModule
	 */
	public WaitKeystoreState(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	@Override
	public void proceed() {
		resetTimeout(ScAuthModule.TIMEOUT_KEYSTOREINIT);
	}

	@Override
	public AbstractState transition(ScAuthStateTransition trs) {
		switch (trs) {
		case PASSWORD_REQUEST:
			return scAuthModule.getStateInstance(OpenKeyStoreState.class);
		case ABORT_AUTH:
			return scAuthModule.getStateInstance(IdleState.class);
		default:
			return super.transition(trs);
		}
	}
}