
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
	enum NetworkManagerState {
		NOT_CONNECTED, CONNECTING, CONNECTING_VPN, CONNECTED
	}

	void start();

	void stop();

	public NetworkManagerState getState();

	public void addListener(Listener l);

	public void removeListener(Listener l);

	public interface Listener {
		void stateChanged(NetworkManagerState old, NetworkManagerState state);

	}
}
