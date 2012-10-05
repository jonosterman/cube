package ch.admin.vbs.cube.core.network;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** manage the base VPN to cube network when connected from the Internet */
public class CubeVPNManager {
	private static final Logger LOG = LoggerFactory.getLogger(CubeVPNManager.class);
	private Executor exec = Executors.newCachedThreadPool();
	private final CubeVPNManagerCallback callback;

	public CubeVPNManager(CubeVPNManagerCallback callback) {
		this.callback = callback;
	}

	public void openVPN() {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(5000);					
				} catch (Exception e) {
					LOG.error("Failed to invoke VPN openning");
				}
			}
		});
	}

	public void closeVPN() {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					LOG.error("Failed to invoke VPN closing");
				}
			}
		});
	}

	public interface CubeVPNManagerCallback {
		void vpnOpened();

		void vpnFailed();
	}

}
