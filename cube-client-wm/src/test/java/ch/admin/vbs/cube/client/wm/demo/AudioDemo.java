package ch.admin.vbs.cube.client.wm.demo;

import org.junit.Assert;

import ch.admin.vbs.cube.client.wm.ui.dialog.AudioDialog;
import ch.admin.vbs.cube.core.MockContainerUtil;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmAudioControl;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

public class AudioDemo {
	private static final int CONNECT_VBOXWS_TIMEOUT = 30000;
	private VBoxProduct vbox;
	private MockContainerUtil util = new MockContainerUtil();

	public void startAndConnectVboxsrv() throws Exception {
		// connect web service
		connect();
		// register a new VM
		Vm vm = util.createTestVm("A");
		vbox.registerVm(vm, null);
		// start VM
		vbox.startVm(vm, null);
		Thread.sleep(1000);
		VmAudioControl ctrl = new VmAudioControl();
		AudioDialog dial = new AudioDialog(null, "test-id-A", ctrl);
		dial.displayWizard();
		// stop VM
		vbox.poweroffVm(vm, null);
		// unregister VM
		vbox.unregisterVm(vm, null);
		// disconnect web service
		vbox.stop();
	}

	public void connect() throws Exception {
		vbox = new VBoxProduct();
		vbox.start();
		// wait until connected
		long timeout = System.currentTimeMillis() + CONNECT_VBOXWS_TIMEOUT;
		while (!vbox.isConnected()) {
			if (System.currentTimeMillis() > timeout) {
				Assert.assertTrue("Not able to connect VirtualBox WebService", false);
			}
			Thread.sleep(500);
		}
		System.out.println("Connect to VirtualBox WebService ..... OK");
	}

	public static void main(String[] args) throws Exception {
		new AudioDemo().startAndConnectVboxsrv();
	}
}