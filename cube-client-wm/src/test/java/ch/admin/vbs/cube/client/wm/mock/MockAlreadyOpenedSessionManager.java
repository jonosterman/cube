package ch.admin.vbs.cube.client.wm.mock;

import java.util.ArrayList;
import java.util.List;

import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISessionManager;
import ch.admin.vbs.cube.core.ISessionUI;

public class MockAlreadyOpenedSessionManager implements ISessionManager {
	private ArrayList<ISessionManagerListener> listeners = new ArrayList<ISessionManagerListener>();
	private MockSession session;

	@Override
	public void start() {
		session = new MockSession();
		for (ISessionManagerListener l : listeners) {
			l.sessionOpened(session);
		}
	}

	@Override
	public void addListener(ISessionManagerListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(ISessionManagerListener l) {
	}

	@Override
	public void closeSession(ISession session) {
	}

	@Override
	public List<ISession> getSessions() {
		ArrayList<ISession> list = new ArrayList<ISession>();
		list.add(session);
		return list;
	}

	public void setup(ISessionUI sessionUI) {
	}
}
