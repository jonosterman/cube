package ch.admin.vbs.cube.core.network;

import java.util.Vector;

import org.freedesktop.NMApplet;
import org.freedesktop.NMApplet.NmState;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.network.impl.DBusExplorer;

public class NetworkManagerDBus {
	public enum ActiveConnectionState {
		NM_ACTIVE_CONNECTION_STATE_UNKNOWN, NM_ACTIVE_CONNECTION_STATE_ACTIVATING, NM_ACTIVE_CONNECTION_STATE_ACTIVATED, NM_ACTIVE_CONNECTION_STATE_DEACTIVATING
	}

	public enum VpnConnectionState {
		CUBEVPN_CONNECTION_STATE_PREPARE, CUBEVPN_CONNECTION_STATE_CONNECT, CUBEVPN_CONNECTION_STATE_ACTIVATED, CUBEVPN_CONNECTION_STATE_FAILED, CUBEVPN_CONNECTION_STATE_DISCONNECTED
	}

	public enum CubeVpnConnectionReason {
		NM_VPN_CONNECTION_STATE_REASON_UNKNOWN, NM_VPN_CONNECTION_STATE_REASON_NONE, NM_VPN_CONNECTION_STATE_REASON_USER_DISCONNECTED, NM_VPN_CONNECTION_STATE_REASON_DEVICE_DISCONNECTED, NM_VPN_CONNECTION_STATE_REASON_SERVICE_STOPPED, NM_VPN_CONNECTION_STATE_REASON_IP_CONFIG_INVALID, NM_VPN_CONNECTION_STATE_REASON_CONNECT_TIMEOUT, NM_VPN_CONNECTION_STATE_REASON_SERVICE_START_TIMEOUT, NM_VPN_CONNECTION_STATE_REASON_SERVICE_START_FAILED, NM_VPN_CONNECTION_STATE_REASON_NO_SECRETS, NM_VPN_CONNECTION_STATE_REASON_LOGIN_FAILED, NM_VPN_CONNECTION_STATE_REASON_CONNECTION_REMOVED
	}

	public enum DeviceState {
		NM_DEVICE_STATE_STATE_UNKNOWN, NM_DEVICE_STATE_UNMNAGED, NM_DEVICE_STATE_UNAVAILABLE, NM_DEVICE_STATE_DISCONNECTED, NM_DEVICE_STATE_PREPARE, NM_DEVICE_STATE_CONFIG, NM_DEVICE_STATE_NEED_AUTH, NM_DEVICE_STATE_IP_CONFIG, NM_DEVICE_STATE_ACTIVATED, NM_DEVICE_STATE_FAILED
	}

	public static void main(String[] args) {
		Manager m = new Manager();
		m.start();
	}

	private static final String NM_DBUS_OBJECT = "/org/freedesktop/NetworkManager";
	private static final String NM_DBUS_BUSNAME = "org.freedesktop.NetworkManager";
	private static final String NM_DBUS_NMIFACE = "org.freedesktop.NetworkManager";
	private static final Logger LOG = LoggerFactory.getLogger(NetworkManagerDBus.class);
	private DBusExplorer dbusExplorer;
	private DBusConnection systemCon;
	private DBusConnection sessionCon;
	private boolean connected;
	private Object networkManagerVersion;

	public NetworkManagerDBus() {
	}

	public void start() {
		// create DBusExplorer helper object
		dbusExplorer = new DBusExplorer();
		try {
			// Connect DBUS
			systemCon = DBusConnection.getConnection(DBusConnection.SYSTEM);
			sessionCon = DBusConnection.getConnection(DBusConnection.SESSION);
			connected = true;
			// Dump version in logs
			networkManagerVersion = dbusExplorer.getProperty(systemCon, //
					NM_DBUS_BUSNAME, //
					NM_DBUS_OBJECT, //
					NM_DBUS_NMIFACE, //
					"Version");
			LOG.debug("NetworkManager Version : " + networkManagerVersion);
		} catch (DBusException e) {
			LOG.error("Failed to connect DBUS.", e);
		}
	}

	/**
	 * Add listener to both session and system context
	 * 
	 * @param scope
	 * @param type
	 * @param h
	 * @throws DBusException
	 */
	public <T extends DBusSignal> void addSignalHanlder(int scope, Class<T> type, DBusSigHandler<T> h) throws DBusException {
		switch (scope) {
		case DBusConnection.SESSION:
			sessionCon.addSigHandler(type, h);
			break;
		case DBusConnection.SYSTEM:
		default:
			systemCon.addSigHandler(type, h);
			break;
		}
	}

	@SuppressWarnings("unchecked")
	public <E> E getEnumConstant(int stateId, Class<E> x) {
		if (x.equals(NmState.class)) {
			NmState s = NmState.get(stateId);
			s = s != null ? s : NmState.NM_STATE_UNKNOWN;
			return (E) s;
		}
		if (stateId < 0 || stateId >= x.getEnumConstants().length) {
			return null;
		} else {
			return x.getEnumConstants()[stateId];
		}
	}
}
