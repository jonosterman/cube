package ch.admin.vbs.cube.core.network;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.freedesktop.NetworkManager;
import org.freedesktop.NetworkManager.Device;
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
		NM_DEVICE_STATE_STATE_UNKNOWN(0), //
		NM_DEVICE_STATE_UNMNAGED(10), //
		NM_DEVICE_STATE_UNAVAILABLE(20), //
		NM_DEVICE_STATE_DISCONNECTED(30), //
		NM_DEVICE_STATE_PREPARE(40), //
		NM_DEVICE_STATE_CONFIG(50), //
		NM_DEVICE_STATE_NEED_AUTH(60), //
		NM_DEVICE_STATE_IP_CONFIG(70), //
		NM_DEVICE_STATE_IP_CHECK(80), //
		NM_DEVICE_STATE_SECONDARIES(90), //
		NM_DEVICE_STATE_ACTIVATED(100), //
		NM_DEVICE_STATE_DEACTIVATING(110), //
		NM_DEVICE_STATE_FAILED(120); //
		private static final Map<Integer, DeviceState> lookup = new HashMap<Integer, DeviceState>();
		static {
			for (DeviceState s : EnumSet.allOf(DeviceState.class))
				lookup.put(s.getCode(), s);
		}
		private int code;

		private DeviceState(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}

		public static DeviceState get(int code) {
			return lookup.get(code);
		}
	}
	
	public enum NmState {
		NM_STATE_UNKNOWN(0), //
		NM_STATE_ASLEEP(10), //
		NM_STATE_DISCONNECTED(20), //
		NM_STATE_DISCONNECTING(30), //
		NM_STATE_CONNECTING(40), //
		NM_STATE_CONNECTED_LOCAL(50), //
		NM_STATE_CONNECTED_SITE(60), //
		NM_STATE_CONNECTED_GLOBAL(70); //
		private static final Map<Integer, NmState> lookup = new HashMap<Integer, NmState>();
		static {
			for (NmState s : EnumSet.allOf(NmState.class))
				lookup.put(s.getCode(), s);
		}
		private int code;

		private NmState(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}

		public static NmState get(int code) {
			return lookup.get(code);
		}
	}

	public static void main(String[] args) {
		NetManager m = new NetManager();
		m.start();
	}

	private static final String NM_DBUS_OBJECT = "/org/freedesktop/NetworkManager";
	private static final String NM_DBUS_BUSNAME = "org.freedesktop.NetworkManager";
	private static final String NM_DBUS_NMIFACE = "org.freedesktop.NetworkManager";
	private static final Logger LOG = LoggerFactory
			.getLogger(NetworkManagerDBus.class);
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
			//
			List<Path> x = systemCon.getRemoteObject(NM_DBUS_BUSNAME,
					NM_DBUS_OBJECT, org.freedesktop.NetworkManager.class)
					.GetDevices();
			System.out.println(x.size());
			for (Path p : x) {
				System.out.println(">> " + p);
				Object y = systemCon.getRemoteObject(NM_DBUS_BUSNAME,
						p.getPath());
				Device d = (Device) y;

				System.out.println(d);
			}
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
	public <T extends DBusSignal> void addSignalHanlder(int scope,
			Class<T> type, DBusSigHandler<T> h) throws DBusException {
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
			// NmState use specific event numbers 
			NmState s = NmState.get(stateId);
			s = s != null ? s : NmState.NM_STATE_UNKNOWN;
			return (E) s;
		} else if (x.equals(DeviceState.class)) {
			// DeviceState use specific event numbers 
			DeviceState s = DeviceState.get(stateId);
			s = s != null ? s : DeviceState.NM_DEVICE_STATE_FAILED;
			return (E) s;
		} else { 
			LOG.error("Unsupported constant ["+stateId+" / "+x+"]");
			return null;
		}
	}

	public String getTypeAsString(int type) {
		switch (type) {
		case DBusConnection.SYSTEM:
			return "SYSTEM";
		case DBusConnection.SESSION:
			return "SESSION";
		default:
			return "unkown(" + type + ")";
		}
	}

	public void triggerNetworkManagerRestart() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				LOG.debug("Restart network manager");
				try {
					NetworkManager nm = systemCon.getRemoteObject(NM_DBUS_BUSNAME, NM_DBUS_OBJECT, org.freedesktop.NetworkManager.class);
					nm.Enable(false);
					Thread.sleep(500);
					nm.Enable(true);
				} catch (Exception e) {
					LOG.error("Failed to re-enable NetworkManager", e);
				}
			}
		}).start();		
	}
}
