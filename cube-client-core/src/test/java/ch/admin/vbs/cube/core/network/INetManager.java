package ch.admin.vbs.cube.core.network;

public interface INetManager {
	/** manager state */
	public enum NetState { CONNECTING, CONNECTING_VPN, CONNECTED_DIRECT, CONNECTED_BY_VPN, DEACTIVATED  }
}
