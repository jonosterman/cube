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

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.NMApplet;
import org.freedesktop.NMApplet.NmState;
import org.freedesktop.NMApplet.VpnConnectionState;
import org.freedesktop.NetworkManager;
import org.freedesktop.NetworkManager.StateChanged;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;
import ch.admin.vbs.cube.core.network.INetworkManager;

/**
 * Implements INetworkManager state machine. Use NMApplet in order to monitor
 * system's NetworkManager (via DBUS) and start/stop the Cube VPN.
 * 
 * CubeVPN is a base VPN used to connect Cube network from the Internet. The
 * whole traffic will be tunneled into it, it includes: VM's VPNs and access to
 * Cube web service (https). Therefore all traffic is encrypted two times.
 * 
 */
public class CNMStateMachine implements INetworkManager {
	private static final Logger LOG = LoggerFactory.getLogger(CNMStateMachine.class);
	private NetworkConnectionState curState;
	private ArrayList<Listener> listeners = new ArrayList<INetworkManager.Listener>(2);
	private NMApplet nmApplet = new NMApplet();
	private boolean nmConnected;

	/** Set current state and notify listener about the change */
	private void setCurrentState(NetworkConnectionState n) {
		NetworkConnectionState old = curState;
		curState = n;
		// notify listeners only if state effectively changed
		if (old != n) {
			for (Listener l : listeners) {
				l.stateChanged(old, n);
			}
		}
	}

	@Override
	public void start() {
		// set initial state
		setCurrentState(NetworkConnectionState.NOT_CONNECTED);
		try {
			// connect NetworkManager (via DBUS)
			nmApplet.connect();
			// listen NetworkManager events
			nmApplet.addSignalHanlder(DBusConnection.SYSTEM, StateChanged.class, new StateChangedHandler());
			nmApplet.addListener(new VpnStateChangedHandler());
			// Restart NetworkManager in order to sync this StateMachine and the
			// NetworkManager states.
			new Thread(new Runnable() {
				@Override
				public void run() {
					LOG.debug("Restart network manager");
					try {
						nmApplet.enable(false);
					} catch (Exception e) {
						LOG.error("Failed to disable NetworkManager", e);
					}
					try {
						nmApplet.enable(true);
					} catch (Exception e) {
						LOG.error("Failed to re-enable NetworkManager", e);
					}
				}
			}).start();
		} catch (DBusException e) {
			LOG.error("Failed to connect DBUS", e);
		}
	}

	@Override
	public void stop() {
		// nothing
	}

	@Override
	public NetworkConnectionState getState() {
		return curState;
	}

	@Override
	public void addListener(Listener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(Listener l) {
		listeners.remove(l);
	}
	
	@Override
	public List<String> getNetworkInterfaces() {
		ArrayList<String> list = new ArrayList<String>();
		ShellUtil su = new ShellUtil();
		try {
			su.run(null, ShellUtil.NO_TIMEOUT, "ifconfig");
			for(String line : su.getStandardOutput().toString().split("\n")) {
				if (line.startsWith("eth") || line.startsWith("wlan")|| line.startsWith("usb")) {
					// input validation
					if (line.matches("eth[0-9] .*") || line.matches("wlan[0-9] .*")|| line.matches("usb[0-9] .*")) {
						list.add(line.split(" +",2)[0]);					
					}
				}
			}
		} catch (ShellUtilException e) {
			e.printStackTrace();
		}		
		return list;
	}

	/** DBUS Signal listener */
	public class StateChangedHandler implements DBusSigHandler<NetworkManager.StateChanged> {
		public StateChangedHandler() {
		}

		@Override
		public void handle(NetworkManager.StateChanged signal) {
			synchronized (this) {
				// convert signal into the corresponding enumeration reference
				NmState sig = nmApplet.getEnumConstant(signal.state.intValue(), NmState.class);
				LOG.debug("Got DBus signal [NetworkManager.StateChanged] - [{}]", sig);	
				if (sig == null) {
					LOG.error("Unknown NmState signal [{}].",signal.state.intValue());
					return;
				}
				//
				LOG.error("Process NmState signal [{}].",sig);
				switch (sig) {
				case NM_STATE_CONNECTED:
					// set connected flag. see
					// VpnStateChangedHandler.handle(...) below.
					nmConnected = true;
					// start CubeVPN and update current state.
					checkVpnNeeded();
					break;
				case NM_STATE_CONNECTING:
					// set connected flag. see
					// VpnStateChangedHandler.handle(...) below.
					nmConnected = false;
					// set current state
					setCurrentState(NetworkConnectionState.CONNECTING);
					break;
				case NM_STATE_ASLEEP:
				case NM_STATE_UNKNOWN:
				default:
					// set connected flag. see
					// VpnStateChangedHandler.handle(...) below.
					nmConnected = false;
					// set current state
					setCurrentState(NetworkConnectionState.NOT_CONNECTED);
					// ensure that VPN is closed
					nmApplet.closeVpn();
					break;
				}
			}
		}
	}

	/** VPN state's changes listener */
	public class VpnStateChangedHandler implements NMApplet.VpnStateListener {
		public void handle(VpnConnectionState sig) {
			synchronized (this) {
				LOG.debug("Got CubeVPN signal - [{}]", sig);
				switch (sig) {
				case CUBEVPN_CONNECTION_STATE_ACTIVATED:
					// VPN established
					if (nmConnected) {
						setCurrentState(NetworkConnectionState.CONNECTED_TO_CUBE_BY_VPN);
					} else {
						// Network Manager is not connected. We should not have
						// any VPN running.
						nmApplet.closeVpn();
					}
					break;
				case CUBEVPN_CONNECTION_STATE_CONNECT:
				case CUBEVPN_CONNECTION_STATE_PREPARE:
					// VPN connecting ...
					if (nmConnected) {
						setCurrentState(NetworkConnectionState.CONNECTING_VPN);
					} else {
						// Network Manager is not connected. We should not have
						// any VPN running
						nmApplet.closeVpn();
					}
					break;
				default:
					// VPN disconnected
					if (nmConnected) {
						// reconnect again and again (as long as needed)
						checkVpnNeeded();
					}
					break;
				}
			}
		}
	}

	/**
	 * Once connected to a network, we have to find out if we need to start the
	 * Cube VPN or if we can directly reach the Cube Server. We only check the
	 * IP in order if we got an IP in the right range. It will not work in a
	 * network with the same IP range than Cube, but we do not care since the
	 * VPN will not work either in this case.
	 * 
	 * This method set the State Machine's state accordingly and eventually
	 * start the VPN.
	 */
	private void checkVpnNeeded() {
		if (nmApplet.isIpReachable(CubeClientCoreProperties.getProperty(VPN_IP_CHECK_PROPERTIE))) {
			LOG.debug("We are connected to cube network. No need to open CubeVPN.");
			// we may connect cube server directly.
			setCurrentState(NetworkConnectionState.CONNECTED_TO_CUBE);
		} else {
			LOG.debug("Connected to foreign network. Start CubeVPN.");
			setCurrentState(NetworkConnectionState.CONNECTING_VPN);
			// we have to start CubeVPN
			try {
				nmApplet.startVpn();
			} catch (Exception e) {
				LOG.error("VPN not connected. Will wait network manager to reconnect.", e);
				setCurrentState(NetworkConnectionState.NOT_CONNECTED);
			}
		}
	}
}
