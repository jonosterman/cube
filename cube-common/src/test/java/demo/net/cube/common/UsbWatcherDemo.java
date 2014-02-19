package demo.net.cube.common;

import java.util.Enumeration;

import mock.net.cube.common.MockTokenPassphraseCallback;
import net.cube.token.ITokenDeviceListener;
import net.cube.token.TokenDeviceEvent;
import net.cube.token.UsbTokenDevice;

/**
 * Access sample configuration values from XML
 */
public class UsbWatcherDemo {
	public static void main(String[] args) throws Exception {
		MockTokenPassphraseCallback cb = new MockTokenPassphraseCallback("123456");
		UsbTokenDevice device = new UsbTokenDevice();
		device.addListener(new ITokenDeviceListener() {
			@Override
			public void handle(TokenDeviceEvent event) {
				System.out.println("Handle > " + event.getType() + " : " + event.getToken().getSubjectName());
				try {
					Enumeration<String> as = event.getToken().getBuilder().getKeyStore().aliases();
					// fire KS event
					while (as.hasMoreElements()) {
						String alias = as.nextElement();
						System.out.println("> alias: " + alias);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		device.setup(cb);
		device.start();
		Thread.sleep(30000);
		System.out.println("done.");
	}
}
