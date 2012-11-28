package ch.admin.vbs.cube3.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.impl.TokenEvent;
import ch.admin.vbs.cube3.core.ILogin;
import ch.admin.vbs.cube3.core.ILogin.Event;
import ch.admin.vbs.cube3.core.ILogin.ILoginListener;
import ch.admin.vbs.cube3.core.ISessionMgr;

public class SessionMgr implements ISessionMgr, ILoginListener, ITokenListener {
	private ArrayList<ISessionsChangeListener> listeners = new ArrayList<ISessionMgr.ISessionsChangeListener>(2);
	private Lock sessionsLock = new ReentrantLock();
	private HashMap<String, Session> sessions = new HashMap<String, Session>();
	private Session activeSession;
	private IContainerFactory cFactory;
	// ===============================================
	// Implements ILoginListener
	// ===============================================
	@Override
	public void processEvent(Event e, IIdentityToken id) {
		sessionsLock.lock();
		try {
			switch (e) {
			case AUTHENTIFICATION_FAILURE:
				break;
			case AUTHENTIFICATION_FAILURE_MAX:
				break;
			case USER_AUTHENTICATED:
				if (activeSession != null) {
					activeSession.lock();
					activeSession = null;
				}
				activeSession = sessions.get(id.getUuid());
				if (activeSession == null) {
					activeSession = new Session();
					sessions.put(id.getUuid(), activeSession);
					activeSession.init(id, cFactory);
				}
				activeSession.activate();
				break;
			default:
				break;
			}
		} finally {
			sessionsLock.unlock();
		}
	}

	// ===============================================
	// IoC
	// ===============================================
	public void setup(ILogin login, ITokenDevice device, IContainerFactory cFactory) {
		this.cFactory = cFactory;
		login.addListener(this);
		device.addListener(this);
	}

	public void start() {
	}

	// ===============================================
	// Implements ITokenDevice
	// ===============================================
	@Override
	public void notifyTokenEvent(TokenEvent event) {
		sessionsLock.lock();
		try {
			// handle new request
			switch (event.getType()) {
			case TOKEN_INSERTED:
				// nothing. wait until ILoginListener notify authentication.
				break;
			case TOKEN_REMOVED:
				if (activeSession != null) {
					activeSession.lock();
					activeSession = null;
				}
				break;
			}
		} finally {
			sessionsLock.unlock();
		}		
	}
	// ===============================================
	// Implements ISessionMgr
	// ===============================================
	@Override
	public void addListener(ISessionsChangeListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(ISessionsChangeListener l) {
		listeners.remove(l);
	}
}
