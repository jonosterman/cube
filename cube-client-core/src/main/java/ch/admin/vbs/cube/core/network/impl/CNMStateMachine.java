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

import org.freedesktop.NMApplet;
import org.freedesktop.NMApplet.DeviceState;
import org.freedesktop.NMApplet.NmState;
import org.freedesktop.NMApplet.VpnConnectionState;
import org.freedesktop.NetworkManager;
import org.freedesktop.NetworkManager.StateChanged;
import org.freedesktop.NetworkManager.VPN.Connection.VpnStateChanged;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.network.INetworkManager;

/**
 *
 */
public class CNMStateMachine implements INetworkManager {
	private static final Logger LOG = LoggerFactory.getLogger(CNMStateMachine.class);
	private NetworkConnectionState curState;
	private ArrayList<Listener> listeners = new ArrayList<INetworkManager.Listener>(2);
	private NMApplet nmApplet = new NMApplet();

	private enum CNMStateEvent {
		NM_CONNECTING, NM_CONNECTED, NM_DISCONNECTED, VPN_CONNECTING, VPN_CONNECTED, VPN_DISCONNECTED
	}

	private void setCurrentState(NetworkConnectionState n) {
		NetworkConnectionState old = curState;
		curState = n;
		if (old != n) {
			for (Listener l : listeners) {
				l.stateChanged(old, n);
			}
		}
	}

	@Override
	public void start() {
		setCurrentState(NetworkConnectionState.NOT_CONNECTED);
		// TODO: adapt curState to NetworkManager status
		try {
			// create DBUS interface to NetworkManager
			nmApplet.connect();
			// register for signal (connections and VPN connections)
			nmApplet.addSignalHanlder(DBusConnection.SYSTEM, StateChanged.class, new StateChangedHandler());
			nmApplet.addSignalHanlder(DBusConnection.SYSTEM, VpnStateChanged.class, new VpnStateChangedHandler());
			nmApplet.addSignalHanlder(DBusConnection.SYSTEM, org.freedesktop.NetworkManager.Device.StateChanged.class, new DeviceStateChangedHandler());
			// initial NetworkManager stop/start in order to get its state through events
			new Thread(new Runnable() {
				@Override
				public void run() {
					try { nmApplet.enable(false); } catch (Exception e) {}
					try { nmApplet.enable(true); } catch (Exception e) {}				}
			});
		} catch (DBusException e) {
			LOG.error("Failed to connect DBUS", e);
		}
		// ...
	}

	@Override
	public void stop() {
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

	public class StateChangedHandler implements DBusSigHandler<NetworkManager.StateChanged> {
		public StateChangedHandler() {
		}

		@Override
		public void handle(NetworkManager.StateChanged signal) {
			NmState sig = nmApplet.getEnumConstant(signal.state.intValue(), NmState.class);
			LOG.debug("Got DBus signal [NetworkManager.StateChanged] - [{}]", sig);
			switch (sig) {
			case NM_STATE_CONNECTED:
				process(CNMStateEvent.NM_CONNECTED);
				break;
			case NM_STATE_CONNECTING:
				process(CNMStateEvent.NM_CONNECTING);
				break;
			case NM_STATE_ASLEEP:
			case NM_STATE_UNKNOWN:
			default:
				process(CNMStateEvent.NM_DISCONNECTED);
				break;
			}
		}
	}

	public class DeviceStateChangedHandler implements DBusSigHandler<NetworkManager.Device.StateChanged> {
		public DeviceStateChangedHandler() {
		}

		@Override
		public void handle(NetworkManager.Device.StateChanged signal) {
			DeviceState sigo = nmApplet.getEnumConstant(signal.ostate.intValue(), DeviceState.class);
			DeviceState sign = nmApplet.getEnumConstant(signal.nstate.intValue(), DeviceState.class);
			LOG.debug("Got DBus signal [Device.StateChanged] - [{} -> {}]  (ignored)", sigo, sign);
			// actually we do not use device StateChanged event. We only rely on
			// NetworkManager StateChange events.
		}
	}

	public class VpnStateChangedHandler implements DBusSigHandler<VpnStateChanged> {
		public VpnStateChangedHandler() {
		}

		@Override
		public void handle(VpnStateChanged signal) {
			VpnConnectionState sig = nmApplet.getEnumConstant(signal.state.intValue(), VpnConnectionState.class);
			LOG.debug("Got DBus signal [VpnStateChanged] - [{}]", sig);
			switch (sig) {
			case NM_VPN_CONNECTION_STATE_ACTIVATED:
				process(CNMStateEvent.VPN_CONNECTED);
				break;
			case NM_VPN_CONNECTION_STATE_PREPARE:
			case NM_VPN_CONNECTION_STATE_NEED_AUTH:
			case NM_VPN_CONNECTION_STATE_CONNECT:
			case NM_VPN_CONNECTION_STATE_IP_CONFIG_GET:
				process(CNMStateEvent.VPN_CONNECTING);
				break;
			default:
				process(CNMStateEvent.VPN_DISCONNECTED);
				break;
			}
		}
	}

	private void process(CNMStateEvent action) {
		LOG.debug("process action [{}]", action);
		synchronized (this) {
			switch (action) {
			case NM_CONNECTING:
				setCurrentState(NetworkConnectionState.CONNECTING);
				break;
			case NM_CONNECTED:
				checkVpnNeeded();
				break;
			case NM_DISCONNECTED:
				setCurrentState(NetworkConnectionState.NOT_CONNECTED);
				break;
			case VPN_CONNECTING:
				setCurrentState(NetworkConnectionState.CONNECTING_VPN);
				break;
			case VPN_CONNECTED:
				setCurrentState(NetworkConnectionState.CONNECTED);
				break;
			case VPN_DISCONNECTED:
				checkVpnNeeded();
				break;
			default:
				break;
			}
		}
	}

	private void checkVpnNeeded() {
		if (nmApplet.isIpReachable("172.20.0.5")) {
			LOG.debug("We are connected to cube network. No need to open CubeVPN.");
			// we may connect cube server directly.
			setCurrentState(NetworkConnectionState.CONNECTED);
		} else {
			LOG.debug("Connected to foreign network. Start CubeVPN.");
			setCurrentState(NetworkConnectionState.CONNECTING_VPN);
			// we have to start CubeVPN
			Path vpn = nmApplet.startVpn();
			if (vpn == null) {
				LOG.debug("VPN not connected. Will wait network manager to reconnect.");
				setCurrentState(NetworkConnectionState.NOT_CONNECTED);
			}
		}
	}
}
