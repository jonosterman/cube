package ch.admin.vbs.cube.core.network;

import org.freedesktop.NMApplet.NmState;
import org.freedesktop.NetworkManager;
import org.freedesktop.NetworkManager.StateChanged;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Manager {
	private static final Logger LOG = LoggerFactory.getLogger(Manager.class);
	private NetworkManagerDBus dbus;

	public Manager() {
		dbus = new NetworkManagerDBus();
	}

	public void start() {
		dbus.start();
		try {
			dbus.addSignalHanlder(DBusConnection.SYSTEM, StateChanged.class, new StateChangedHandler());
		} catch (DBusException e) {
			LOG.error("Failed to add signal hanlder", e);
		}
	}

	/** DBUS Signal listener */
	public class StateChangedHandler implements DBusSigHandler<NetworkManager.StateChanged> {
		public StateChangedHandler() {
		}

		@Override
		public void handle(NetworkManager.StateChanged signal) {
			synchronized (this) {
				// convert signal into the corresponding enumeration reference
				NmState sig = dbus.getEnumConstant(signal.state.intValue(), NmState.class);
				if (sig == null) {
					// Unknown signal. Specification has chaged?
					LOG.error("Unknown signal [NetworkManager.StateChanged: {}].", signal.state.intValue());
					return;
				}
				//
				LOG.error("Process signal [NetworkManager.StateChanged: {}].", sig);
			}
		}
	}

	public static void main(String[] args) {
		Manager m = new Manager();
		m.start();
	}
}
