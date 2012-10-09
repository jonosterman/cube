package ch.admin.vbs.cube.core.network.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;

/** manage the base VPN to cube network when connected from the Internet */
public class CubeVPNManager {
	private static final Logger LOG = LoggerFactory.getLogger(CubeVPNManager.class);
	private Executor exec = Executors.newCachedThreadPool();
	private final CubeVPNManagerCallback callback;
	private Lock lock = new ReentrantLock();

	public CubeVPNManager(CubeVPNManagerCallback callback) {
		this.callback = callback;
	}

	public void openVPN() {
		// prevent starting VPN while closing one
		lock.lock();
		lock.unlock();
		// execute VPN in another thread
		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					/*
					 * do not use network manager to start VPN anymore. system
					 * network-manager is unable to start it (seems to be a bug)
					 * and user network manager need to run network manager
					 * applet in background (will display unwanted status
					 * pop-ups over cube UI)
					 */
					LOG.debug("Open Cube VPN");
					ScriptUtil script = new ScriptUtil();
					ShellUtil su = script.execute("sudo", "./vpn-open.pl", //
							"--tap", CubeClientCoreProperties.getProperty("INetworkManager.vpnTap"),//
							"--hostname", CubeClientCoreProperties.getProperty("INetworkManager.vpnServer"),//
							"--port", CubeClientCoreProperties.getProperty("INetworkManager.vpnPort"),//
							"--ca", CubeClientCoreProperties.getProperty("INetworkManager.vpnCa"),//
							"--cert", CubeClientCoreProperties.getProperty("INetworkManager.vpnCrt"),//
							"--key", CubeClientCoreProperties.getProperty("INetworkManager.vpnKey"), //
							"--no-lzo", //
							"--no-bridge" //
					);
					if (su.getExitValue() == 0) {
						LOG.debug("VPN successfully opened");
						// notify VPN status change
						callback.vpnOpened();
					} else {
						LOG.error("Failed to start Cube VPN [script returned {}]. ", su.getExitValue());
						su.dumpOutputInLogs(LOG);
						// notify VPN status change
						callback.vpnOpenFailed();
					}
				} catch (Exception e) {
					LOG.error("Failed to start Cube VPN", e);
					// notify VPN status change
					callback.vpnOpenFailed();
				}
			}
		});
	}

	public void closeVPN() {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				lock.lock();
				try {
					LOG.debug("Close Cube VPN");
					ScriptUtil script = new ScriptUtil();
					ShellUtil su = script.execute("sudo", "./vpn-close.pl", //
							"--tap", CubeClientCoreProperties.getProperty("INetworkManager.vpnTap"),//
							"--no-bridge" //
					);
					if (su.getExitValue() == 0) {
						// dump output
						LOG.debug("VPN closed");
						// notify callback
						callback.vpnClosed();
						// debug...
						su.dumpOutputInLogs(LOG);
					} else {
						// dump output
						LOG.debug("Failed to close VPN");
						su.dumpOutputInLogs(LOG);
						// notify callback
						callback.vpnCloseFailed();
					}
				} catch (Exception e) {
					LOG.error("Failed to close VPN", e);
					// notify callback
					callback.vpnCloseFailed();
				} finally {
					lock.unlock();
				}
			}
		});
	}

	public interface CubeVPNManagerCallback {
		void vpnOpened();

		void vpnCloseFailed();

		void vpnClosed();

		void vpnOpenFailed();
	}
}
