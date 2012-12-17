package ch.admin.vbs.cube3.core.impl;

import java.util.ArrayList;

import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.network.INetManager;
import ch.admin.vbs.cube.core.network.INetManager.Listener;
import ch.admin.vbs.cube.core.network.INetManager.NetState;
import ch.admin.vbs.cube3.core.ILogin;
import ch.admin.vbs.cube3.core.ILogin.ILoginListener;
import ch.admin.vbs.cube3.core.ILoginUI;
import ch.admin.vbs.cube3.core.IWSClient;
import ch.admin.vbs.cube3.core.IWSConnection;

public class WSClient implements IWSClient, ILoginListener, Listener {
	private ArrayList<IWSClientListener> listeners = new ArrayList<IWSClient.IWSClientListener>();
	private IWSConnection ws;

	// ===============================================
	// Implements NetManager.Listener
	// ===============================================
	public void stateChanged(NetState old, NetState state) {
	};

	// ===============================================
	// Implements IWSClient
	// ===============================================
	@Override
	public void addListener(IWSClientListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(IWSClientListener l) {
		listeners.remove(l);
	}

	// ===============================================
	// Implements ILoginListener
	// ===============================================
	@Override
	public void processEvent(ch.admin.vbs.cube3.core.ILogin.Event e, IIdentityToken id) {
		switch (e) {
		case AUTHENTIFICATION_FAILURE:
		case AUTHENTIFICATION_FAILURE_MAX:
			ws.stop();
			break;
		case USER_AUTHENTICATED:
			ws.restart(id);
			break;
		default:
			break;
		}
	}

	// ==================================================
	// IoC
	// ==================================================
	public void setup(ILogin login, IWSConnection ws, INetManager net) {
		this.ws = ws;
		login.addListener(this);
		net.addListener(this);
	}

	public void start() {
	}
}
