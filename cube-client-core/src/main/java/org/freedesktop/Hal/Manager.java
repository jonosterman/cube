package org.freedesktop.Hal;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.exceptions.DBusException;

public interface Manager extends DBusInterface {
	/**
	 * DeviceAdded signal
	 */
	public class DeviceAdded extends DBusSignal {
		public final String obj;

		/**
		 * Default constructor
		 * 
		 * @param obj
		 *            the UDI of the object which generated the signal (always
		 *            org.freedesktop.Hal.Manager in this case)
		 * @param udi
		 *            the UDI of the newly added object
		 * @throws DBusException
		 */
		public DeviceAdded(String path,String obj) throws DBusException {
			super(path, obj);
			this.obj = obj;
		}
	}

	/**
	 * DeviceRemoved signal
	 */
	public class DeviceRemoved extends DBusSignal {
		public final String obj;


		/**
		 * Default constructor
		 * 
		 * @param obj
		 *            the UDI of the object which generated the signal (always
		 *            org.freedesktop.Hal.Manager in this case)
		 * @param udi
		 *            the UDI of the removed object
		 * @throws DBusException
		 */
		public DeviceRemoved(String path,String obj) throws DBusException {
			super(path,obj);
			this.obj = obj;
		}
	}

	/**
	 * FindDeviceByCapability method
	 */
	public String[] FindDeviceByCapability(String capability);

	/**
	 * FindDeviceStringMatch method
	 */
	public String[] FindDeviceStringMatch(String key, String value);

	/**
	 * GetAllDevice method
	 */
	public String[] GetAllDevices();
}
