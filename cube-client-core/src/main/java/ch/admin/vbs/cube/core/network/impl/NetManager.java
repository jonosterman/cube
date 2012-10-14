/**
 * Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.admin.vbs.cube.core.network.impl;

import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.freedesktop.NetworkManager.Device;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.CubeClientCoreProperties;
import ch.admin.vbs.cube.core.network.INetManager;
import ch.admin.vbs.cube.core.network.impl.CubeVPNManager.CubeVPNManagerCallback;
import ch.admin.vbs.cube.core.network.impl.NetworkManagerDBus.DeviceState;

/** see method 'processEvent()' for details about this class. */
public class NetManager implements INetManager, Runnable, CubeVPNManagerCallback {
	/**
	 * IP to check in order to known if we are connected to Cube network or if
	 * we need to start the VPN.
	 */
	public static final String VPN_IP_CHECK_PROPERTIE = "INetworkManager.vpnIpCheck";
	private static final Logger LOG = LoggerFactory.getLogger(NetManager.class);
	private NetworkManagerDBus dbus;
	private NetState netState = NetState.DEACTIVATED;
	private LinkedList<DeviceState> queue = new LinkedList<DeviceState>();
	private CubeVPNManager cubeVpnManager;
	private ArrayList<Listener> listeners = new ArrayList<INetManager.Listener>(2);
	private Lock nmLock = new ReentrantLock();
	private Executor exec = Executors.newCachedThreadPool();

	public NetManager() {
		dbus = new NetworkManagerDBus();
		cubeVpnManager = new CubeVPNManager(this);
	}

	@Override
	public NetState getState() {
		return netState;
	}

	/** Start NetManager thread. */
	public void start() {
		// start DBUS interface
		dbus.start();
		// start queue processing thread
		Thread t = new Thread(this, "NetManager");
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
	public void stop() {
		// nothing
	}

	@Override
	public void addListener(Listener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(Listener l) {
		listeners.remove(l);
	}

	private void changeStateAndNotifyListener(final NetState newState) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				NetState oldState = netState;
				netState = newState;
				// notify listeners only if state effectively changed
				if (oldState != newState) {
					for (Listener l : listeners) {
						l.stateChanged(oldState, newState);
					}
				}
			}
		});
	}

	@Override
	public List<String> getNetworkInterfaces() {
		ArrayList<String> list = new ArrayList<String>();
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				if (!intf.getName().startsWith("tap") && !intf.getName().startsWith("tun") && !intf.getName().equals("lo")) {
					list.add(intf.getName());
				}
			}
		} catch (IOException e) {
			LOG.error("Failed to list interfaces", e);
		}
		return list;
	}

	@Override
	public void run() {
		// proceed queue
		DeviceState event = null;
		while (true) {
			synchronized (queue) {
				event = queue.pollFirst();
			}
			if (event == null) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOG.error("Failed to sleep", e);
				}
			} else {
				processEvent(event);
				event = null;
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
		nmLock.lock();
		try {
			switch (deviceState) {
			case NM_DEVICE_STATE_ACTIVATED:
			case NM_DEVICE_STATE_UNAVAILABLE:
				LOG.debug("Kill VPN.");
				cubeVpnManager.closeVPN();
				if (dbus.isNetworkManagerActive()) {
					LOG.debug("NetworkManager is active.");
					if (isCubeNetworkAccessible()) {
						// no need of further configuration, server is
						// accessible.
						LOG.debug("Connected to CUBE. VPN is not needed.");
						changeStateAndNotifyListener(NetState.CONNECTED_DIRECT);
					} else {
						// start VPN
						LOG.debug("Start VPN.");
						changeStateAndNotifyListener(NetState.CONNECTING_VPN);
						cubeVpnManager.openVPN();
					}
				} else {
					LOG.debug("NetworkManager is not active (could not connect anything).");
					changeStateAndNotifyListener(NetState.DEACTIVATED);
				}
				break;
			default:
				break;
			}
		} catch (Exception e) {
			nmLock.unlock();
			LOG.error("Failed to process event.", e);
		}
	}

	/**
	 * convert IP to int. (10.11.1.2 --> 0x0a0b0102)
	 */
	private int ipToInt(String ip) {
		String[] split = ip.split("\\.");
		return ((Integer.parseInt(split[0]) & 0xFF) << 24) | //
				((Integer.parseInt(split[1]) & 0xFF) << 16) | //
				((Integer.parseInt(split[2]) & 0xFF) << 8) //
				| (Integer.parseInt(split[3]) & 0xFF);
	}

	private boolean isCubeNetworkAccessible() {
		// get IP to check from configuration file
		int ipToCheck = ipToInt(CubeClientCoreProperties.getProperty(VPN_IP_CHECK_PROPERTIE));
		try {
			// go through all phyiscal interfaces
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				if (intf.getName().startsWith("tap") || intf.getName().startsWith("tun") || intf.getName().equals("lo")) {
					// skip tap, tun, lo
					LOG.debug("Skip interface [" + intf.getName() + "]");
					continue;
				}
				// go through all logical interfaces
				for (InterfaceAddress nic : intf.getInterfaceAddresses()) {
					byte[] arr = nic.getAddress().getAddress();
					// only handle IPv4 since cube is still IPv4
					if (arr.length == 4 && nic.getNetworkPrefixLength() > 0) {
						LOG.debug("check IP address [" + intf.getName() + "," + nic.getAddress() + "]");
						int nicIp = (arr[0] << 24) & 0xff000000 | (arr[1] << 16) & 0x00ff0000 | (arr[2] << 8) & 0x0000ff00 | arr[3] & 0x000000ff;
						int mask = (0xffffffff) << (32 - nic.getNetworkPrefixLength());
						if ((nicIp & mask) == (ipToCheck & mask)) {
							return true;
						}
					}
				}
			}
		} catch (SocketException e) {
			LOG.error("Failed to list network interfaces", e);
		}
		return false;
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

	@Override
	public void vpnOpenFailed() {
		nmLock.lock();
		try {
			if (netState == NetState.CONNECTING_VPN) {
				LOG.debug("VPN connection failed. Retry.");
				cubeVpnManager.openVPN();
			} else {
				// bad state
				LOG.debug("VPN is connetced but NetManager state is [" + netState + "]. Close VPN.");
				cubeVpnManager.closeVPN();
			}
		} catch (Exception e) {
			LOG.error("Failed to update NetManager state", e);
		}
	}

	@Override
	public void vpnOpened() {
		nmLock.lock();
		try {
			if (netState == NetState.CONNECTING_VPN) {
				LOG.debug("VPN connected");
				netState = NetState.CONNECTED_BY_VPN;
			} else {
				// bad state
				LOG.debug("VPN is connetced but NetManager state is [" + netState + "]. Close VPN.");
				cubeVpnManager.closeVPN();
			}
		} catch (Exception e) {
			LOG.error("Failed to update NetManager state", e);
		}
	}

	@Override
	public void vpnClosed() {
		// ignore
	}

	@Override
	public void vpnCloseFailed() {
		// ignore
	}
}
