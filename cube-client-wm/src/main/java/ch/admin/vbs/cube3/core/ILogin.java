package ch.admin.vbs.cube3.core;

import ch.admin.vbs.cube.common.keyring.IIdentityToken;

public interface ILogin {
	/*
	 * AUTHENTIFICATION_FAILURE_MAX: authentication failed AND max failure count
	 * has been reached. you need to remove the token before new attempt.
	 * 
	 * AUTHENTIFICATION_FAILURE: authentication failed.
	 * 
	 * USER_AUTHENTICATED: authentication succeed.
	 */
	public enum Event {
		USER_AUTHENTICATED, AUTHENTIFICATION_FAILURE, AUTHENTIFICATION_FAILURE_MAX
	}

	void addListener(ILoginListener l);

	void removeListener(ILoginListener l);

	interface ILoginListener {
		void processEvent(Event e, IIdentityToken id);
	}
}
