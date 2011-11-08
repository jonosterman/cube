
package org.freedesktop;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Variant;

public interface NetworkManagerSettings extends DBusInterface {
	List<Path> ListConnections();

	public interface Connection extends DBusInterface {
		Map<String, Map<String,Variant<?>>> GetSettings();
	}

}
