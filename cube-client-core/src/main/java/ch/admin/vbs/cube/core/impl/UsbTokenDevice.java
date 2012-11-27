package ch.admin.vbs.cube.core.impl;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.freedesktop.Hal.Device;
import org.freedesktop.Hal.Manager;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.impl.TokenEvent;
import ch.admin.vbs.cube.core.impl.TokenEvent.EventType;

/**
 * Implements a token device through a USB Stick with a p12 keystore file.  
 * @author Manuel Wyss
 */
public class UsbTokenDevice implements ITokenDevice {
	private static final Logger LOG = LoggerFactory.getLogger(UsbTokenDevice.class);
	private ArrayList<ITokenListener> listeners = new ArrayList<ITokenListener>();
	private ArrayList<KeyPathListener> kpListeners = new ArrayList<KeyPathListener>();
	private AtomicBoolean tokenState = new AtomicBoolean();
	private String mountPoint;
	private String volId = null;

	@Override
	public boolean isTokenReady() {
		return tokenState.get();
	}

	@Override
	public void addListener(ITokenListener l) {
		listeners.add(l);
	}

	public void addKeyPathListener(KeyPathListener l) {
		kpListeners.add(l);
	}

	public boolean checkInserted(DBusConnection conn) {
		try {
			Manager m = conn.getRemoteObject("org.freedesktop.Hal", "/org/freedesktop/Hal/Manager", Manager.class);
			for (String s : m.GetAllDevices()) {
				Map p = getProperties(s, conn);
				if (p != null && p.get("volume.uuid") != null) {
					Object path = p.get("linux.sysfs_path");
					if (path != null && path.toString().contains('/' + "usb")) {
						mountPoint = getMountPoint(p);
						String k = null;
						if ((k = isUsbToken(mountPoint)) != null) {
							volId = s;
							fireStateChanged(true);
							fireKeyPath(mountPoint + "/" + k);
							return true;
						} else {
							LOG.info("Attached USB does not contain .p12 key");
							umount(mountPoint);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private String getMountPoint(Map p) {
		if (mountPoint != null) {
			return mountPoint;
		}
		Object mp = p.get("volume.mount_point");
		if (mp.toString().equals("[]")) {
			ShellUtil pacmd = new ShellUtil();
			final String dev = p.get("block.device").toString().replace("[", "").replace("]", "");
			try {
				pacmd.run(new ArrayList<String>() {
					{
						add("pmount");
						add(dev);
					}
				});
			} catch (Exception e) {
			}
			mp = "/media" + dev.substring(dev.lastIndexOf("/")).replace("]", "");
		}
		return mp.toString().replace("[", "").replace("]", "");
	}

	@Override
	public void start() {
		try {
			final DBusConnection conn = DBusConnection.getConnection(DBusConnection.SYSTEM);
			if (checkInserted(conn))
				fireStateChanged(true);
			conn.addSigHandler(Manager.DeviceAdded.class, new DBusSigHandler<Manager.DeviceAdded>() {
				@Override
				public void handle(Manager.DeviceAdded added) {
					try {
						if (!tokenState.get()) {
							String dev = (String) added.getParameters()[0];
							Map p = getProperties(dev, conn);
							if (p.get("volume.uuid") != null) {
								mountPoint = getMountPoint(p);
								String k = null;
								if ((k = isUsbToken(mountPoint)) != null) {
									volId = dev;
									fireStateChanged(true);
									fireKeyPath(mountPoint + "/" + k);
								} else {
									LOG.info("Attached USB does not contain .p12 key");
									umount(mountPoint);
								}
							}
						}
					} catch (Exception e) {
						LOG.error(e.getMessage());
					}
				}
			});
			conn.addSigHandler(Manager.DeviceRemoved.class, new DBusSigHandler<Manager.DeviceRemoved>() {
				@Override
				public void handle(Manager.DeviceRemoved removed) {
					try {
						if (tokenState.get()) {
							String dev = (String) removed.getParameters()[0];
							if (dev.contains("volume")) {
								if (dev.equals(volId)) {
									fireStateChanged(false);
								}
							}
						}
					} catch (Exception e) {
						LOG.error(e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void umount(final String mountPoint) {
		try {
			ShellUtil pacmd = new ShellUtil();
			pacmd.run(new ArrayList<String>() {
				{
					add("pumount");
					add(mountPoint);
				}
			});
			this.mountPoint = null;
			volId = null;
		} catch (Exception e) {
		}
	}

	private String isUsbToken(final String mountPoint) {
		try {
			ShellUtil pacmd = new ShellUtil();
			pacmd.run(new ArrayList<String>() {
				{
					add("ls");
					add(mountPoint);
				}
			});
			String l = pacmd.getStandardOutput().toString();
			if (l != null) {
				StringReader r = new StringReader(l);
				BufferedReader br = new BufferedReader(r);
				String inp = null;
				while ((inp = br.readLine()) != null) {
					if (inp.contains(".p12"))
						return inp;
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	private Map getProperties(String device, DBusConnection conn) {
		Map p = null;
		try {
			Device d = conn.getRemoteObject("org.freedesktop.Hal", device, Device.class);
			if (d != null) {
				p = d.GetAllProperties();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return p;
	}

	private void fireStateChanged(boolean newState) {
		LOG.debug("Token state changed [{}]. Fire event.", newState);
		tokenState.set(newState);
		TokenEvent event = new TokenEvent(newState ? EventType.TOKEN_INSERTED : EventType.TOKEN_REMOVED);
		for (ITokenListener l : listeners) {
			l.notifyTokenEvent(event);
		}
		if (!newState) {
			umount(getMountPoint(null));
		}
	}

	private void fireKeyPath(String keyPath) {
		LOG.debug("Keypath changed [{}]. Fire event.", keyPath);
		for (KeyPathListener l : kpListeners) {
			l.onKeyPathEvent(keyPath);
		}
	}

	public interface KeyPathListener {
		public void onKeyPathEvent(String keyPath);
	}
}
