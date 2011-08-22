package ch.admin.vbs.cube.core.impl.scauthmodule;


class WaitKeystoreAndPasswordState extends AbstractState {
	/**
	 * 
	 */
	private final ScAuthModule scAuthModule;

	/**
	 * @param scAuthModule
	 */
	public WaitKeystoreAndPasswordState(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	@Override
	public void proceed() {
		resetTimeout(ScAuthModule.TIMEOUT_KEYSTOREINIT);
		scAuthModule.exec.execute(scAuthModule.getOpenKeyStoreTask());
	}

	@Override
	public AbstractState transition(ScAuthStateTransition trs) {
		switch (trs) {
		case PASSWORD_SUBMIT:
			return scAuthModule.getStateInstance(WaitKeystoreState.class);
		case PASSWORD_REQUEST:
			return scAuthModule.getStateInstance(WaitPasswordState.class);
		case ABORT_AUTH:
			return scAuthModule.getStateInstance(IdleState.class);
		default:
			return super.transition(trs);
		}
	}
}