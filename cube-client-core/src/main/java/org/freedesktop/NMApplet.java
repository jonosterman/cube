
package org.freedesktop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.freedesktop.DBus.Properties;
import org.freedesktop.NetworkManager.VPN.Connection;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see http://projects.gnome.org/NetworkManager//developers/api/08/spec-08.html
 */
public class NMApplet {
	private static final String NM_DBUS_OBJECT = "/org/freedesktop/NetworkManager";
	private static final String NM_DBUS_ADDRESS = "org.freedesktop.NetworkManager";
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

	private DBusConnection conn;
	private NetworkManager networkManager;
	private Properties properties;

	public void connect() throws DBusException {
		// Connect system's dbus
		conn = DBusConnection.getConnection(DBusConnection.SYSTEM);
		networkManager = (NetworkManager) conn.getRemoteObject(NM_DBUS_ADDRESS, NM_DBUS_OBJECT, NetworkManager.class);
		properties = (Properties) conn.getRemoteObject(NM_DBUS_ADDRESS, NM_DBUS_OBJECT, Properties.class);
		// Add signal handlers to react on connections changes
		conn.addSigHandler(NetworkManager.StateChanged.class, new Client());
		conn.addSigHandler(Connection.VpnStateChanged.class, new VpnClient());
		//
		LOG.debug("NMApplet Connected");
	}

	private class VpnClient implements DBusSigHandler<Connection.VpnStateChanged> {
		public VpnClient() {
		}

		@Override
		public void handle(Connection.VpnStateChanged signal) {
			VpnConnectionState sig = getEnumConstant(signal.state.intValue(), VpnConnectionState.class);
			VpnConnectionReason res = getEnumConstant(signal.reason.intValue(), VpnConnectionReason.class);
			System.out.println("Signal(Connection.VpnStateChanged) [" + sig + "][" + res + "]");
		}
	}

	private class Client implements DBusSigHandler<NetworkManager.StateChanged> {
		@Override
		public void handle(NetworkManager.StateChanged signal) {
			NmState sig = getEnumConstant(signal.state.intValue(), NmState.class);
			System.out.println("Signal(NetworkManager.StateChanged) [" + sig + "]");
		}
	}

	public NmState getState() {
		int s = networkManager.state().intValue();
		return getEnumConstant(s, NmState.class);
	}

	private <E> E getEnumConstant(int stateId, Class<E> x) {
		if (stateId < 0 || stateId >= x.getEnumConstants().length) {
			return null;
		} else {
			return x.getEnumConstants()[stateId];
		}
	}

	public List<Path> getDevices() {
		return networkManager.GetDevices();
	}

	public NmDevice getDevice(Path p) throws DBusException {
		Properties devProps = (Properties) conn.getRemoteObject(NM_DBUS_ADDRESS, p.toString(), Properties.class);
		Map<String, Variant> x = devProps.GetAll("");
		NmDevice d = new NmDevice(p, (Boolean) x.get("Carrier").getValue(), (String) x.get("HwAddress").getValue(),
				((UInt32) x.get("Speed").getValue()).intValue());
		return d;
	}

	public ActiveConnection getActiveConnection(Path p) throws DBusException {
		Properties devProps = (Properties) conn.getRemoteObject(NM_DBUS_ADDRESS, p.toString(), Properties.class);
		return new ActiveConnection(p, devProps.GetAll(""));
	}

	public boolean isWirelessEnabled() {
		return "true".equalsIgnoreCase(properties.Get(NM_DBUS_ADDRESS, "WirelessEnabled").toString());
	}

	public boolean isWirelessHardwareEnabled() {
		return "true".equalsIgnoreCase(properties.Get(NM_DBUS_ADDRESS, "WirelessHardwareEnabled").toString());
	}

	public List<Path> getActiveConnections() {
		return properties.Get(NM_DBUS_ADDRESS, "ActiveConnections");
	}

	
	public class ActiveConnection {
		private final Path path;
		private final boolean def;
		private final List<NmDevice> devices;
		private final String servicename;
		private final ActiveConnectionState state;
		private final boolean vpn;
		private final VpnConnectionState vpnState;

		@SuppressWarnings("unchecked")
		public ActiveConnection(Path path, Map<String, Variant> map) {
			this.path = path;
			this.def = (Boolean) map.get("Default").getValue();
			this.devices = new ArrayList<NmDevice>();
			for (Path x : (Vector<Path>) map.get("Devices").getValue()) {
				try {
					devices.add(getDevice(x));
				} catch (DBusException e) {
					LOG.error("Failed to retrieve device [" + x + "]", e);
				}
			}
			this.servicename = (String) map.get("ServiceName").getValue();
			this.state = getEnumConstant(((UInt32) map.get("State").getValue()).intValue(), ActiveConnectionState.class);
			this.vpn = (Boolean) map.get("Vpn").getValue();
			this.vpnState = map.containsKey("VpnState") ? //
			getEnumConstant( //
					((UInt32) map.get("VpnState").getValue()).intValue() //
					, VpnConnectionState.class //
			) //
					: null;
		}

		public Path getPath() {
			return path;
		}

		public boolean isDef() {
			return def;
		}

		public List<NmDevice> getDevices() {
			return devices;
		}

		public String getServicename() {
			return servicename;
		}

		public ActiveConnectionState getState() {
			return state;
		}

		public boolean isVpn() {
			return vpn;
		}

		public VpnConnectionState getVpnState() {
			return vpnState;
		}

		@Override
		public String toString() {
			return String.format("ActiveConnection [path:%s, vpn:%b, (vpn)state:%s]", path.getPath(), vpn, vpn ? vpnState : state);
		}
	}

	public class NmDevice {
		private final int speed;
		private final String mac;
		private final boolean carrier;
		private final Path path;

		public NmDevice(Path path, boolean carrier, String mac, int speed) {
			this.path = path;
			this.carrier = carrier;
			this.mac = mac;
			this.speed = speed;
		}

		public int getSpeed() {
			return speed;
		}

		public String getMac() {
			return mac;
		}

		public boolean isCarrier() {
			return carrier;
		}

		public String toString() {
			return String.format("Device[carrier:%b, mac:%s, speed:%d, path:%s]", carrier, mac, speed, path.toString());
		}

		public Path getPath() {
			return path;
		}
	}
}
