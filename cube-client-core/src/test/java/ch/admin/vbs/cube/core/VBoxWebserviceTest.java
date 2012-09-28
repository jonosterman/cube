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
package ch.admin.vbs.cube.core;

import org.junit.Assert;
import org.junit.Test;

import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

/**
 * This test is used to stress-test VirtualBox interface. It is a tricky part
 * since VirtualBox locking / timeout behavior is still a little bit random.. Of
 * course you need VirtualBox installed on the test machine. The test VM (tiny
 * core Linux) disk is included in cube repository.
 * 
 */
public class VBoxWebserviceTest {
	private static final int CONNECT_VBOXWS_TIMEOUT = 30000;
	private VBoxProduct vbox;
	private MockContainerUtil util = new MockContainerUtil();

	@Test
	public void startAndConnectVboxsrv() throws Exception {
		// connect web service
		connect();
		// register a new VM
		Vm vm = util.createTestVm("A");
		vbox.registerVm(vm, null);
		// start VM
		vbox.startVm(vm, null);
		Thread.sleep(5000);
		// save & restore cycle
		for (int i = 0; i < 5; i++) {
			vbox.save(vm, null);
			Thread.sleep(2000);
			vbox.startVm(vm, null);
			Thread.sleep(5000);
		}
		// stop VM
		vbox.poweroffVm(vm, null);
		// unregister VM
		vbox.unregisterVm(vm, null);
		// disconnect web service
		disconnect();
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

	public void disconnect() {
		vbox.stop();
	}

	public static void main(String[] args) throws Exception {
		new VBoxWebserviceTest().startAndConnectVboxsrv();
	}
}
