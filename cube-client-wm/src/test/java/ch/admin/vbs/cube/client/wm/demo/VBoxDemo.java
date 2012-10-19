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
package ch.admin.vbs.cube.client.wm.demo;

import org.junit.Assert;
import org.virtualbox_4_2.IMachine;

import ch.admin.vbs.cube.core.MockContainerUtil;
import ch.admin.vbs.cube.core.vm.NicOption;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

public class VBoxDemo {
	private static final int CONNECT_VBOXWS_TIMEOUT = 30000;
	private VBoxProduct vbox;
	private MockContainerUtil util = new MockContainerUtil();

	public void startAndConnectVboxsrv() throws Exception {
		// connect web service
		connect();
		// register a new VM
		Vm vm = util.createTestVm("A");
		vbox.registerVm(vm, null);
		// check register
		for (IMachine machine : vbox.getMachines() ) {
			System.out.println("#################");
			System.out.printf("Machine [ID:%s][ HWUUID:%s]\n",machine.getId(),machine.getHardwareUUID());
			System.out.println("#################");
		}
		// start VM
		vbox.startVm(vm, null);
		Thread.sleep(1000);		
		vbox.connectNic(vm, new NicOption("eth0"));		
		// stop VM
		vbox.poweroffVm(vm, null);
		// unregister VM
		vbox.unregisterVm(vm, null);
		// disconnect web service
		vbox.stop();
		Thread.sleep(2000);
		System.exit(0);
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
		new VBoxDemo().startAndConnectVboxsrv();
	}
}