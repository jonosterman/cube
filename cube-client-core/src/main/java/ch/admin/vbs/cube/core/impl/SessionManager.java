/**
 * Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.admin.vbs.cube.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.core.ILogin;
import ch.admin.vbs.cube.core.ILoginListener;
import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISessionManager;
import ch.admin.vbs.cube.core.ISessionUI;
import ch.admin.vbs.cube.core.network.INetworkManager;
import ch.admin.vbs.cube.core.network.INetworkManager.Listener;
import ch.admin.vbs.cube.core.network.INetworkManager.NetworkConnectionState;
import ch.admin.vbs.cube.core.vm.VmController;

public class SessionManager implements ISessionManager, ILoginListener {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);
	private ILogin login;
	private HashMap<String, ISession> sessions = new HashMap<String, ISession>();
	private ISessionUI sessionUI;
	private IContainerFactory containerFactory;
	private VmController vmController;
	private ArrayList<ISessionManagerListener> listeners = new ArrayList<ISessionManager.ISessionManagerListener>(2);
	private INetworkManager networkManager;

	public SessionManager() {
		vmController = new VmController();
	}

	@Override
	public void start() {
		networkManager.start();
		vmController.start();
	}

	@Override
	public void addListener(ISessionManagerListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(ISessionManagerListener l) {
		listeners.remove(l);
	}

	private void fireSessionOpened(ISession session) {
		for (ISessionManagerListener l : listeners) {
			l.sessionOpened(session);
		}
	}

	private void fireSessionClosed(ISession session) {
		for (ISessionManagerListener l : listeners) {
			l.sessionClosed(session);
		}
	}

	private void fireSessionLocked(ISession session) {
		for (ISessionManagerListener l : listeners) {
			l.sessionLocked(session);
		}
	}

	@Override
	public void closeSession(ISession session) {
		LOG.debug("Close session [{}]", session.getId().getSubjectName());
		synchronized (sessions) {
			sessions.remove(session.getId().getUuid());
		}
		LOG.debug("call session.close() [{}]", session.getId().getSubjectName());
		session.close();
		LOG.debug("login.discardAuth [{}]", session.getId().getSubjectName());
		login.discardAuthentication(session.getId());
		LOG.debug("fireSessionClosed [{}]", session.getId().getSubjectName());
		fireSessionClosed(session);
	}

	@Override
	public List<ISession> getSessions() {
		synchronized (sessions) {
			ArrayList<ISession> all = new ArrayList<ISession>();
			for (ISession o : sessions.values()) {
				all.add(o);
			}
			return all;
		}
	}

	// =====================================================
	// ILoginListener
	// =====================================================
	@Override
	public void userAuthentified(IIdentityToken id) {
		LOG.debug("Notified that a user has been authenticated.");
		synchronized (this) {
			ISession ses = sessions.get(id.getUuid());
			if (ses == null) {
				LOG.debug("Initialize a new session for this user");
				ses = new Session(id, sessionUI, vmController);
				ses.setContainerFactory(containerFactory);
				synchronized (sessions) {
					sessions.put(id.getUuid(), ses);
				}
			} else {
				LOG.debug("Refresh already locked session of this user");
				// update the ID in the session (it holds the new keystore)
				ses.setId(id);
			}
			LOG.debug("Open user's session");
			ses.open();
			LOG.debug("Notify listeners about the opened session");
			fireSessionOpened(ses);
		}
	}

	@Override
	public void userLocked(IIdentityToken id) {
		ISession ses = sessions.get(id.getUuid());
		if (ses != null) {
			LOG.debug("Lock session [{}]", ses.getId().getSubjectName());
			ses.lock();
			fireSessionLocked(ses);
		}
	}

	@Override
	public void userLogedOut(IIdentityToken id) {
		ISession ses = null;
		synchronized (sessions) {
			ses = sessions.get(id.getUuid());
			if (ses != null) {
				closeSession(ses);
			}
		}
	}

	// Dependencies injection
	public void setup(ILogin login, ISessionUI sessionUI, IContainerFactory containerFactory, INetworkManager networkManager) {
		this.login = login;
		this.sessionUI = sessionUI;
		this.containerFactory = containerFactory;
		this.login.addListener(this);
		this.networkManager = networkManager;
		vmController.setNetworkManager(networkManager);
		networkManager.addListener(new Listener() {
			@Override
			public void stateChanged(NetworkConnectionState old, NetworkConnectionState state) {
				SessionManager.this.sessionUI.notifyConnectionState(state);
				// also notify all sessions about connection change since each session try to connect to cube web-service
				synchronized (sessions) {
					for (ISession o : sessions.values()) {
						o.notifyConnectionState(state);
					}
				}
			}
		});
	}
}
