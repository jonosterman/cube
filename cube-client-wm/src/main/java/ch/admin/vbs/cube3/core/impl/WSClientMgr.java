package ch.admin.vbs.cube3.core.impl;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.cube.wsclient.WebServiceClient;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.network.INetManager;
import ch.admin.vbs.cube.core.network.INetManager.Listener;
import ch.admin.vbs.cube.core.network.INetManager.NetState;
import ch.admin.vbs.cube3.core.ILogin;
import ch.admin.vbs.cube3.core.ILogin.ILoginListener;
import ch.admin.vbs.cube3.core.IWSClientMgr;

public class WSClientMgr implements IWSClientMgr, ILoginListener, Listener, ITokenListener {
	private static final Logger LOG = LoggerFactory.getLogger(WSClientMgr.class);
	private ArrayList<IWSClientListener> listeners = new ArrayList<IWSClientMgr.IWSClientListener>();
	private boolean isTokenReady = false;
	private boolean isNetworkReady = false;
	private HashMap<String, WebServiceClient> webservices = new HashMap<String, WebServiceClient>();
	private WebServiceClient current;
	private boolean isLoginReady;

	private synchronized void restartWebService(IIdentityToken id) {
		// desactive current webservice 
		if (current != null) {
			current.setActive(false);
		}
		// active/create webservice for current user
		WebServiceClient ws = webservices.get(id.getUuid());
		if (ws == null) {
			ws = new WebServiceClient(id);
			ws.start();
			webservices.put(id.getUuid(), ws);
		}
		current = ws;
		//
		pool();
	}

	public void pool() {
		if (current != null) {
			LOG.debug("pool (net:"+isNetworkReady+", token:"+isTokenReady+", login:"+isLoginReady+") -> setActive({})",isNetworkReady && isTokenReady && isLoginReady );			
			current.setActive(isNetworkReady && isTokenReady && isLoginReady);
		} else {
			LOG.debug("No webservice currently activated");
		}
	}

	// ===============================================
	// Implements ITokenListener
	// ===============================================
	public void notifyTokenEvent(ch.admin.vbs.cube.core.impl.TokenEvent event) {
		switch (event.getType()) {
		case TOKEN_INSERTED:
			isTokenReady = true;
			break;
		case TOKEN_REMOVED:
			isTokenReady = false;
			break;
		}
		pool();
	}

	// ===============================================
	// Implements NetManager.Listener
	// ===============================================
	public void stateChanged(NetState old, NetState state) {
		switch (state) {
		case CONNECTED_BY_VPN:
		case CONNECTED_DIRECT:
			isNetworkReady = true;
			break;
		case CONNECTING:
		case CONNECTING_VPN:
		case DEACTIVATED:
		default:
			isNetworkReady = false;
			break;
		}
		pool();
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
			isLoginReady = false;
			break;
		case USER_AUTHENTICATED:
			isLoginReady = true;
			restartWebService(id);
			break;
		default:
			break;
		}
		pool();
	}

	// ==================================================
	// IoC
	// ==================================================
	public void setup(ILogin login, INetManager net, ITokenDevice token) {
		login.addListener(this);
		net.addListener(this);
		token.addListener(this);
	}

	public void start() {
	}
}
