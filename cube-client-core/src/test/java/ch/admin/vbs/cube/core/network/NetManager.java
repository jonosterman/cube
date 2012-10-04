package ch.admin.vbs.cube.core.network;

import java.util.LinkedList;

import org.freedesktop.NetworkManager.Device;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.network.NetworkManagerDBus.DeviceState;

public class NetManager implements INetManager ,Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(NetManager.class);
	private NetworkManagerDBus dbus;
	private NetState netState = NetState.DEACTIVATED;
	private LinkedList<DeviceState> queue = new LinkedList<DeviceState>();
	private CubeVPNManager cubeVpnManager;

	public NetManager() {
		dbus = new NetworkManagerDBus();
		cubeVpnManager = new CubeVPNManager();
	}

	public NetState getNetState() {
		return netState;
	}

	public void start() {
		// start DBUS interface
		dbus.start();
		// start queue processing thread
		Thread t = new Thread(this,"NetManager");
		t.setDaemon(true);
		t.start();
		// register events
		try {
			dbus.addSignalHanlder(DBusConnection.SYSTEM, org.freedesktop.NetworkManager.Device.StateChanged.class, new DeviceStateChangedHandler(
					DBusConnection.SYSTEM));
		} catch (DBusException e) {
			LOG.error("Failed to add signal handlers. Networking will not work correctly.", e);
		}
		// restart system network manager to sync state
		dbus.triggerNetworkManagerRestart();
	}
	
	@Override
	public void run() {
		// proceed queue
		DeviceState event = null;
		while(true) {
			synchronized (queue) {
				event = queue.pollFirst();
				if (event == null) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						LOG.error("Failed to sleep",e);
					}
				} else {
					processEvent(event);
					event = null;
				}
			}
		}
	}

	/**
	 * Process DBus event and react following this strategy:
	 * 
	 * <pre>
	 * got event:
	 * A) NM_DEVICE_STATE_UNAVAILABLE -> a device was shutdown
	 * B) NM_DEVICE_STATE_ACTIVATED -> a device is connected
	 * C) other events -> device is being proceeded.
	 * --> in both A) & B) cases perform routine:
	 * 1) kill VPN
	 * 2) check NM connected
	 * 3)      yes -->
	 * 4)           check if cube network available (not tap or lo)
	 * 5)                yes --> CONNECTED_DIRECT
	 * 6)                no  --> CONNECTING_VPN
	 * 7)                VPN succeed?
	 * 8)                      yes --> CONNECTED
	 * 9)                      no  --> CONNECTING_VPN
	 * 10)      no  --> NOT_CONNECTED
	 * --> in case C) just notify listener to display some progress icon (and update status line?)
	 * 
	 * Remarks:
	 * - not perfect (vpn may be restarted even if still valid)
	 * - will trigger extra dhcp request on guest if cable disconnect is
	 *   used each time
	 * - robust (at each VPN restart it will ensure its validity)
	 * - generate event and queue it. Process queue in another thread.
	 * - process queue sequentially. it is listener responsibility to return quickly
	 * </pre>
	 * 
	 * @param deviceState
	 */
	protected void processEvent(DeviceState deviceState) {
		System.out.println(">> "+deviceState);
		switch (deviceState) {
		case NM_DEVICE_STATE_ACTIVATED:
		case NM_DEVICE_STATE_UNAVAILABLE:			
			break;
		default:
			break;
		}
	}
	
	public class DeviceStateChangedHandler implements DBusSigHandler<Device.StateChanged> {
		public final int type;

		public DeviceStateChangedHandler(int type) {
			this.type = type;
		}

		@Override
		public void handle(Device.StateChanged state) {
			synchronized (queue) {
				queue.addLast(dbus.getEnumConstant(state.nstate.intValue(), DeviceState.class));
			}
		}
	}

	public static void main(String[] args) {
		NetManager m = new NetManager();
		m.start();
	}
}
