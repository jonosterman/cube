package ch.admin.vbs.cube.core.network;

import org.freedesktop.NetworkManager;
import org.freedesktop.NetworkManager.Device;
import org.freedesktop.NetworkManager.DeviceAdded;
import org.freedesktop.NetworkManager.StateChanged;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.network.NetworkManagerDBus.DeviceState;
import ch.admin.vbs.cube.core.network.NetworkManagerDBus.NmState;

public class Manager {
	private static final Logger LOG = LoggerFactory.getLogger(Manager.class);
	private NetworkManagerDBus dbus;

	public Manager() {
		dbus = new NetworkManagerDBus();
	}

	public void start() {
		dbus.start();
		try {
			dbus.addSignalHanlder(DBusConnection.SYSTEM, StateChanged.class, new StateChangedHandler(DBusConnection.SYSTEM));
			dbus.addSignalHanlder(DBusConnection.SESSION, StateChanged.class, new StateChangedHandler(DBusConnection.SESSION));
			dbus.addSignalHanlder(DBusConnection.SYSTEM, DeviceAdded.class, new DeviceAddedHandler(DBusConnection.SYSTEM));
			dbus.addSignalHanlder(DBusConnection.SESSION, DeviceAdded.class, new DeviceAddedHandler(DBusConnection.SESSION));
			dbus.addSignalHanlder(DBusConnection.SYSTEM, org.freedesktop.NetworkManager.Device.StateChanged.class, new DeviceStateChangedHandler(DBusConnection.SYSTEM));
		} catch (DBusException e) {
			LOG.error("Failed to add signal hanlder", e);
		}
	}

	/** DBUS Signal listener */
	public class StateChangedHandler implements DBusSigHandler<NetworkManager.StateChanged> {
		private final int type;

		public StateChangedHandler(int type) {
			this.type = type;
		}

		@Override
		public void handle(NetworkManager.StateChanged signal) {
			synchronized (this) {
				// convert signal into the corresponding enumeration reference
				NmState sig = dbus.getEnumConstant(signal.state.intValue(), NmState.class);
				if (sig == null) {
					// Unknown signal. Specification has chaged?
					LOG.error("Unknown signal ["+dbus.getTypeAsString(type)+"] [NetworkManager.StateChanged: {}].", signal.state.intValue());
					return;
				}
				//
				LOG.error("Process signal ["+dbus.getTypeAsString(type)+"] [NetworkManager.StateChanged: {}].", sig);
			}
		}
	}

	/** DBUS Signal listener */
	public class DeviceAddedHandler implements DBusSigHandler<NetworkManager.DeviceAdded> {
		private final int type;

		public DeviceAddedHandler(int type) {
			this.type = type;
		}

		@Override
		public void handle(NetworkManager.DeviceAdded device) {
			synchronized (this) {				
				LOG.error("Process signal ["+dbus.getTypeAsString(type)+"] [NetworkManager.DeviceAdded: {}].", device);
			}
		}
	}

	
	public class DeviceStateChangedHandler implements DBusSigHandler<Device.StateChanged> {
		private final int type;

		public DeviceStateChangedHandler(int type) {
			this.type = type;
		}

		@Override
		public void handle(Device.StateChanged state) {
			synchronized (this) {
				LOG.error("Process signal ["+dbus.getTypeAsString(type)+"] ["+dbus.getEnumConstant(state.nstate.intValue(), DeviceState.class)+"] [NetworkManager.DeviceStateChangedHandler: {}].", state);
			}
		}
	}
	public static void main(String[] args) {
		Manager m = new Manager();
		m.start();
	}
}
