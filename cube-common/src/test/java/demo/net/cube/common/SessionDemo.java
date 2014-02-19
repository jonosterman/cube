package demo.net.cube.common;

import mock.net.cube.common.MockTokenPassphraseCallback;
import net.cube.session.SessionManager;
import net.cube.token.UsbTokenDevice;

/**
 * Access sample configuration values from XML
 */
public class SessionDemo {
	public static void main(String[] args) throws Exception {
		MockTokenPassphraseCallback cb = new MockTokenPassphraseCallback("123456");
		UsbTokenDevice device = new UsbTokenDevice();
		SessionManager sessionMgr = new SessionManager();
		sessionMgr.setup(device);
		device.setup(cb);
		device.start();
		Thread.sleep(60000);
		System.out.println("done.");
	}
}
