
package org.freedesktop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.freedesktop.DBus.Properties;
import org.freedesktop.NetworkManagerSettings.Connection;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ch.admin.vbs.cube.core.network.impl.DBusExplorer;

/**
 * @see http://projects.gnome.org/NetworkManager//developers/api/08/spec-08.html
 */
public class NMApplet {
	private static final String NM_DBUS_OBJECT = "/org/freedesktop/NetworkManager";
	private static final String NM_DBUS_BUSNAME = "org.freedesktop.NetworkManager";
	private static final String NM_DBUS_NMIFACE = "org.freedesktop.NetworkManager";
	private static final Logger LOG = LoggerFactory.getLogger(NMApplet.class);

	public enum NmState {
		NM_STATE_UNKNOWN, NM_STATE_ASLEEP, NM_STATE_CONNECTING, NM_STATE_CONNECTED, NM_STATE_DISCONNECTED
	}

	public enum ActiveConnectionState {
		NM_ACTIVE_CONNECTION_STATE_UNKNOWN, NM_ACTIVE_CONNECTION_STATE_ACTIVATING, NM_ACTIVE_CONNECTION_STATE_ACTIVATED, NM_ACTIVE_CONNECTION_STATE_DEACTIVATING
	}

	public enum VpnConnectionState {
		NM_VPN_CONNECTION_STATE_UNKNOWN, NM_VPN_CONNECTION_STATE_PREPARE, NM_VPN_CONNECTION_STATE_NEED_AUTH, NM_VPN_CONNECTION_STATE_CONNECT, NM_VPN_CONNECTION_STATE_IP_CONFIG_GET, NM_VPN_CONNECTION_STATE_ACTIVATED, NM_VPN_CONNECTION_STATE_FAILED, NM_VPN_CONNECTION_STATE_DISCONNECTED
	}

	public enum VpnConnectionReason {
		NM_VPN_CONNECTION_STATE_REASON_UNKNOWN, NM_VPN_CONNECTION_STATE_REASON_NONE, NM_VPN_CONNECTION_STATE_REASON_USER_DISCONNECTED, NM_VPN_CONNECTION_STATE_REASON_DEVICE_DISCONNECTED, NM_VPN_CONNECTION_STATE_REASON_SERVICE_STOPPED, NM_VPN_CONNECTION_STATE_REASON_IP_CONFIG_INVALID, NM_VPN_CONNECTION_STATE_REASON_CONNECT_TIMEOUT, NM_VPN_CONNECTION_STATE_REASON_SERVICE_START_TIMEOUT, NM_VPN_CONNECTION_STATE_REASON_SERVICE_START_FAILED, NM_VPN_CONNECTION_STATE_REASON_NO_SECRETS, NM_VPN_CONNECTION_STATE_REASON_LOGIN_FAILED, NM_VPN_CONNECTION_STATE_REASON_CONNECTION_REMOVED
	}

	public enum DeviceState {
		NM_DEVICE_STATE_STATE_UNKNOWN, NM_DEVICE_STATE_UNMNAGED, NM_DEVICE_STATE_UNAVAILABLE, NM_DEVICE_STATE_DISCONNECTED, NM_DEVICE_STATE_PREPARE, NM_DEVICE_STATE_CONFIG, NM_DEVICE_STATE_NEED_AUTH, NM_DEVICE_STATE_IP_CONFIG, NM_DEVICE_STATE_ACTIVATED, NM_DEVICE_STATE_FAILED
	}

	private DBusConnection sysConn; // system dbus
	private DBusConnection sesConn; // session dbus
	private DBusExplorer explo;

	public NMApplet() {
	}

	/**
	 * Connect DBUS
	 * 
	 * @throws DBusException
	 */
	public void connect() throws DBusException {
		sysConn = DBusConnection.getConnection(DBusConnection.SYSTEM);
		sesConn = DBusConnection.getConnection(DBusConnection.SESSION);
		explo = new DBusExplorer();
	}

	public <T extends DBusSignal> void addSignalHanlder(int scope, Class<T> type, DBusSigHandler<T> h) throws DBusException {
		switch (scope) {
		case DBusConnection.SESSION:
			sesConn.addSigHandler(type, h);
			break;
		case DBusConnection.SYSTEM:
		default:
			sysConn.addSigHandler(type, h);
			break;
		}
	}

	public <E> E getEnumConstant(int stateId, Class<E> x) {
		if (stateId < 0 || stateId >= x.getEnumConstants().length) {
			return null;
		} else {
			return x.getEnumConstants()[stateId];
		}
	}

	public boolean isIpReachable(String ip) {
		// convert ip to int
		int uip = ipToInt(ip);
		// find all active IP for system connections
		try {
			Vector<Path> activeConnections = explo.getProperty(sysConn, //
					NM_DBUS_BUSNAME, //
					NM_DBUS_OBJECT, //
					NM_DBUS_NMIFACE, //
					"ActiveConnections");
			for (Path ac : activeConnections) {
				Vector<Path> devices = explo.getProperty(sysConn, //
						NM_DBUS_BUSNAME, //
						ac.getPath(), //
						"org.freedesktop.NetworkManager.Connection.Active", //
						"Devices");
				for (Path device : devices) {
					Path ip4config = explo.getProperty(sysConn, //
							NM_DBUS_BUSNAME, //
							device.getPath(), //
							"org.freedesktop.NetworkManager.Device", //
							"Ip4Config");
					Vector<Vector<UInt32>> addresses = explo.getProperty(sysConn, //
							NM_DBUS_BUSNAME, //
							ip4config.getPath(), //
							"org.freedesktop.NetworkManager.IP4Config", //
							"Addresses");
					for (Vector<UInt32> address : addresses) {
						boolean match = checkIp(address.get(0), address.get(1).intValue(), uip);
						if (match)
							return true;
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to check IP reachibility", e);
		}
		return false;
	}

	/** check if both IP are in the same network */
	private boolean checkIp(UInt32 uInt32, int mask, int uip) {
		int umask = maskToInt(mask);
		return (uint32ToInt(uInt32) & umask) == (uip & umask);
	}

	public static final int maskToInt(int mask) {
		return ((1 << mask) - 1) << (32 - mask);
	}

	/**
	 * Revert uint32 since NetworkManager deliver the result inverted
	 */
	public static final int uint32ToInt(UInt32 uint32) {
		int uint = uint32.intValue();
		return (uint & 0xff) << 24 | //
				(uint & 0xff00) << 8 | //
				(uint & 0xff0000) >> 8 | //
				(uint & 0xff000000) >> 24;
	}

	/**
	 * convert IP to int. (10.11.1.2 --> 0x0a0b0102)
	 */
	public static final int ipToInt(String ip) {
		String[] split = ip.split("\\.");
		return ((Integer.parseInt(split[0]) & 0xFF) << 24) | //
				((Integer.parseInt(split[1]) & 0xFF) << 16) | //
				((Integer.parseInt(split[2]) & 0xFF) << 8) //
				| (Integer.parseInt(split[3]) & 0xFF);
	}

	public Path startVpn() {
		try {
			/*
			 * get active connection (not a VPN) that will be used as base
			 * connection. If there is more than one active connection we will
			 * have to choose one (uncertain output)
			 */
			Path base = getBaseConnection();
			if (base == null) {
				LOG.debug("No valid base connection found. Do not start VPN.");
				return null;
			}
			// get CubeVpn settings
			Path cubeVpnConnectionPath = getCubeVpnConnectionPath();
			if (cubeVpnConnectionPath == null) {
				LOG.error("No valid Cube VPN defined in user settings.");
				return null;
			}
			// start CubeVpn
			NetworkManager nm = sysConn.getRemoteObject("org.freedesktop.NetworkManager", "/org/freedesktop/NetworkManager",
					org.freedesktop.NetworkManager.class);
			LOG.debug("Activate VPN [{}]",cubeVpnConnectionPath.getPath());
			nm.ActivateConnection( //
					"org.freedesktop.NetworkManagerUserSettings", //
					cubeVpnConnectionPath, //
					new Path("/"), // ignored for VPN
					base); // peek a connection
			// get corresponding Active and monitor it
//			monitorCubeVPN(cubeVpnConnectionPath, hanlder);
			
			return cubeVpnConnectionPath;
		} catch (Exception e) {
			LOG.error("Failed to start VPN", e);
			return null;
		}
	}

//	private void monitorCubeVPN(Path path, DBusSigHandler<VpnStateChanged> handler) throws Exception {
//		Vector<Path> activeConnections = explo.getProperty(sysConn, //
//				NM_DBUS_BUSNAME, //
//				NM_DBUS_OBJECT, //
//				NM_DBUS_NMIFACE, //
//				"ActiveConnections");
//		// filter VPNs (do not use CubeVPN to open CubeVPN....)
//		for (Path p : activeConnections) {
//			Properties properties = explo.getProperties(sysConn, NM_DBUS_BUSNAME, p.getPath());
//			boolean pVpn = properties.Get("org.freedesktop.NetworkManager.Connection.Active", "Vpn");
//			Path conn = properties.Get("org.freedesktop.NetworkManager.Connection.Active", "Connection");
//			if (pVpn && conn.getPath().equals(path.getPath())) {
//				// found the connection
//				LOG.debug("Add signal handler for VPN.");
//				sysConn.addSigHandler(VpnStateChanged.class, handler);		
//			}
//		}
//	}

	private Path getCubeVpnConnectionPath() throws DBusException {
		NetworkManagerSettings settings = sysConn.getRemoteObject("org.freedesktop.NetworkManagerUserSettings", "/org/freedesktop/NetworkManagerSettings",
				org.freedesktop.NetworkManagerSettings.class);
		// list VPN connections in user settings
		for (Path connectionPath : settings.ListConnections()) {
			Connection connection = sysConn.getRemoteObject("org.freedesktop.NetworkManagerUserSettings", connectionPath.getPath(),
					org.freedesktop.NetworkManagerSettings.Connection.class);
			Map<String, Variant<?>> connCfg = connection.GetSettings().get("connection");
			boolean isVpn = "vpn".equals(connCfg.get("type").getValue());
			// debug: select test VPN only
			if (!"test-system-vpn".equals(connCfg.get("id").getValue())) {
				return connectionPath;
			}
		}
		return null;
	}

	public Path getBaseConnection() throws DBusException, SAXException, IOException, ParserConfigurationException {
		Vector<Path> activeConnections = explo.getProperty(sysConn, //
				NM_DBUS_BUSNAME, //
				NM_DBUS_OBJECT, //
				NM_DBUS_NMIFACE, //
				"ActiveConnections");
		// filter VPNs (do not use CubeVPN to open CubeVPN....)
		ArrayList<Path> noVpn = new ArrayList<Path>();
		for (Path p : activeConnections) {
			Properties properties = explo.getProperties(sysConn, NM_DBUS_BUSNAME, p.getPath());
			ActiveConnectionState pState = getEnumConstant(((UInt32) properties.Get("org.freedesktop.NetworkManager.Connection.Active", "State")).intValue(),
					ActiveConnectionState.class);
			boolean pVpn = properties.Get("org.freedesktop.NetworkManager.Connection.Active", "Vpn");
			if (!pVpn && pState == ActiveConnectionState.NM_ACTIVE_CONNECTION_STATE_ACTIVATED) {
				noVpn.add(p);
			} else {
				LOG.debug("Exclude connection [{}]", p.getPath());
			}
		}
		// return a valid connection
		if (noVpn.size() == 1) {
			return noVpn.get(0);
		} else if (noVpn.size() < 1) {
			LOG.debug("No active connection to connect a VPN");
			return null;
		} else {
			LOG.debug("More than 1 active connection to connect a VPN. Use the first one.");
			return noVpn.get(0);
		}
	}
}
