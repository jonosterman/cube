
package ch.admin.vbs.cube.core.impl.scauthmodule;

/**
 * Mother class of all states.
 */
abstract class AbstractState {
	protected long deadline = 0;

	/**
	 * State transitions
	 */
	public static enum ScAuthStateTransition {
		TIMEOUT, // State timeout expiration
		START_AUTH, // Start authentication
		ABORT_AUTH, // Abort authentication
		PASSWORD_SUBMIT, // User submitted his password
		PASSWORD_REQUEST, // KeyStore request user's password
		KEYSTORE_READY;// KeyStore successfully opened
	}

	/**
	 * Apply the given transition to the current state.
	 * 
	 * @return the next state
	 */
	public AbstractState transition(ScAuthStateTransition trs) {
		// default implementation
		ScAuthModule.LOG.error("Invalid transition [{}] [{}]. Ignore.", trs, this);
		return this;
	}

	/**
	 * Proceed state logic here.
	 */
	public void proceed() {
		// default implementation
	}

	/**
	 * Reset state timeout. This timeout will be monitored and enforced by the
	 * StateWatchdog class. Set '0' if state may stay active without time limit.
	 */
	protected void resetTimeout(long timeout) {
		// update deadline with the new timeout
		deadline = timeout == 0 ? 0 : System.currentTimeMillis() + timeout;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}