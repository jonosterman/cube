
package org.freedesktop;

import java.util.List;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.exceptions.DBusException;

public interface NetworkManager extends DBusInterface {

	List<Path> GetDevices();

	UInt32 state();

	void sleep();

	void wake();

	void DeactivateConnection(Path activeConection);

	public class StateChanged extends DBusSignal {
		public final UInt32 state;

		public StateChanged(String path, UInt32 state) throws DBusException {
			super(path, state);
			this.state = state;
		}
	}

	public interface VPN {
		public interface Connection extends DBusInterface {
			public class VpnStateChanged extends DBusSignal {
				public final UInt32 state;
				public final UInt32 reason;

				public VpnStateChanged(String path, UInt32 state, UInt32 reason) throws DBusException {
					super(path);
					this.state = state;
					this.reason = reason;
				}
			}
		}
	}
}
