package ch.admin.vbs.cube.core;

import org.junit.Assert;
import org.junit.Test;

import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

/**
 * Try to start and connect VirtualBox WebService.
 */
public class VBoxWebserviceTest {

	private static final int CONNECT_VBOXWS_TIMEOUT = 30000;

	@Test
	public void startAndConnectVboxsrv() throws Exception {
		VBoxProduct vbox = new VBoxProduct();
		vbox.start();
		// wait until connected
		long timeout = System.currentTimeMillis() + CONNECT_VBOXWS_TIMEOUT;
		while (!vbox.isConnected()) {
			if (System.currentTimeMillis() > timeout) {
				Assert.assertTrue("Not able to connect VirtualBox WebService",
						false);
			}
			Thread.sleep(500);
		}
		System.out.println("Connect to VirtualBox WebService ..... OK");
		vbox.stop();
	}
}
