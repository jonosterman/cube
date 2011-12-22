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

package ch.admin.vbs.cube.core.network;

/**
 * Interface to model network manager function for cube client
 * 
 * <pre>
 * 
 *    [every state]
 *          | in  : disconnected signal (check if still a connection available?) 
 *          | out : kill VM's VPN and cube VPN
 *          V
 * +--------------------+
 * |    NotConnected    |<----------------------------\
 * +--------------------+                             |
 *          | in:  new connection signal              | in  : connection 
 *          | out: nothing (wait NM to retrieve IP)   |       failed.
 *          V                                         | out : nothing.
 * +--------------------+                             |
 * |    Connecting      |-----------------------------'
 * +--------------------+
 *          |
 *          +-------------------------.
 *          |                         | in:  connected signal, but 
 *          | in  : connected and     |      Cube server unreachable
 *          |       Cube server       | out: start Cube VPN
 *          |       reachable.        V
 *          | out : restart VM's   +--------------------+---\ in  : connection failed
 *          |       VPNs.          |   ConnectingVPN    |   | out : reconnect
 *          |                      +--------------------+<--/
 *          |                         | in:  VPN connected
 *          |                         | out: restart VM's VPNs
 *          |   ,---------------------'
 *          V   V                    
 * +--------------------+ 
 * |    Connected       | 
 * +--------------------+
 * </pre>
 */
public interface INetworkManager {
	enum NetworkConnectionState {
		NOT_CONNECTED, CONNECTING, CONNECTING_VPN, CONNECTED_TO_CUBE, CONNECTED_TO_CUBE_BY_VPN
	}
	public static final String VPN_IP_CHECK_PROPERTIE= "INetworkManager.vpnIpCheck";

	void start();

	void stop();

	public NetworkConnectionState getState();

	public void addListener(Listener l);

	public void removeListener(Listener l);

	public interface Listener {
		void stateChanged(NetworkConnectionState old, NetworkConnectionState state);
	}
}
