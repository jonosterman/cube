package ch.admin.vbs.cube.core.impl;

import ch.admin.vbs.cube.core.AbstractUICallback;
import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISessionManager;

public class CallbackLogout extends AbstractUICallback {
	private final ISession session;
	private final ISessionManager smanager;

	public CallbackLogout(ISession session, ISessionManager smanager) {
		this.session = session;
		this.smanager = smanager;
	}

	@Override
	public void process() {
		// close session
		smanager.closeSession(session);
	}

	@Override
	public void aborted() {
	}
}
