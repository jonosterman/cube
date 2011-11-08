
package org.freedesktop;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

public interface NetworkManager extends DBusInterface {
	List<Path> GetDevices();

	UInt32 state();

	void sleep();

	void wake();

	// <method name="DeactivateConnection">
	// <arg name="active_connection" type="o" direction="in"/>
	// </method>
	void DeactivateConnection(Path activeConection);

	// <method name="ActivateConnection">
	// <arg name="service_name" type="s" direction="in"/>
	// <arg name="connection" type="o" direction="in"/>
	// <arg name="device" type="o" direction="in"/>
	// <arg name="specific_object" type="o" direction="in"/>
	// <arg name="active_connection" type="o" direction="out"/>
	// </method>
	Path ActivateConnection(String serviceName, Path connectionToActivate, Path device, Path specificObject);

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

	public interface Connection extends DBusInterface {
		
		//
		public interface Active extends DBusInterface {
			public class PropertiesChanged extends DBusSignal {
				public PropertiesChanged(String path) throws DBusException {
					super(path);
				}
			}
		}
	}

	public interface Device extends DBusInterface {
		public class StateChanged extends DBusSignal {
			public final UInt32 ostate;
			public final UInt32 nstate;
			public final UInt32 reason;

			public StateChanged(String path, UInt32 nstate, UInt32 ostate, UInt32 reason) throws DBusException {
				super(path);
				this.ostate = ostate;
				this.nstate = nstate;
				this.reason = reason;
			}
		}
	}

}
