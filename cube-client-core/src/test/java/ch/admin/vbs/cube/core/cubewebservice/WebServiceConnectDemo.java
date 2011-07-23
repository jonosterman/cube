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

package ch.admin.vbs.cube.core.cubewebservice;

import java.security.KeyStore.Builder;
import java.util.List;

import javax.swing.JOptionPane;

import org.junit.Ignore;
import org.junit.Test;

import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;
import ch.admin.vbs.cube.core.IAuthModuleListener;
import ch.admin.vbs.cube.core.impl.ScAuthModule;
import ch.admin.vbs.cube.core.webservice.WebServiceFactory;
import cube.cubemanager.services.CubeManagerServicePortType;
import cube.cubemanager.services.InstanceDescriptorDTO;
import cube.cubemanager.services.MachineDTO;

public class WebServiceConnectDemo {
	public static void main(String[] args) throws Exception {
		WebServiceConnectDemo d = new WebServiceConnectDemo();
		d.testWebService();
	}

	@Test
	@Ignore
	private void testWebService() throws Exception {
		// / init smartcard stuff
		ScAuthModule auth = new ScAuthModule();
		auth.start();
		auth.openToken();
		auth.addListener(new IAuthModuleListener() {
			@Override
			public void notifyAuthModuleEvent(AuthModuleEvent event) {
				if (event.getType() == AuthEventType.SUCCEED) {
					try {
						connectWS(event.getBuilder());
					} catch (CubeException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Authentication failed ["+event.getType()+"]");
				}
			}
		});
		auth.setPassword(JOptionPane.showInputDialog("PIN").toCharArray());
	}

	private void connectWS(Builder builder) throws CubeException {
		// ///
		WebServiceFactory factory = new WebServiceFactory(builder);
		CubeManagerServicePortType srv = factory.createCubeManagerService();
		//
		System.out.println("List VMs");
		MachineDTO machine = new MachineDTO();
		machine.setMachineUid("<ch.admin.vbs.cube.core.webservice.WebServiceConnectDemo>");
		long t0 = System.currentTimeMillis();
		List<InstanceDescriptorDTO> vms = srv.listUserVm(machine);
		System.out.printf("WebService call in [%d ms].\n", System.currentTimeMillis() - t0);
		// pick a descriptor
		if (vms.size() == 0) {
			System.out.println("No instance found. Could not test staging.");
			System.exit(0);
		}
		System.out.println("[" + vms.size() + "] instances found.");
		// serie
		for (int i = 0; i < 5; i++) {
			t0 = System.currentTimeMillis();
			srv.listUserVm(machine);
			System.out.printf("WebService call in [%d ms].\n", System.currentTimeMillis() - t0);
		}
		// InstanceDescriptorDTO instance = vms.get(0);
		// // get instance configuration
		// InstanceConfigurationDTO config =
		// srv.getInstanceConfiguration(instance.getUuid());
		// // stage VM
		// System.out.println("Create a new instance of the first VM-Template found. Dump its config.");
		// System.out.printf("Instance [%s], version[%s], name [%s]\n",
		// instance.getUuid(), instance.getTemplateVersion(),
		// instance.getName());
		// System.out.println("disk1 size:     "
		// +
		// InstanceParameterHelper.getInstanceParameterAsLong("vbox.disk1Size",
		// config));
		// System.out.println("network config:");
		// System.out.println("         - NIC1 (" + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic1", config) +
		// ", " + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic1Bridge",
		// config) + ", " + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic1Mac", config)
		// + ")");
		// System.out.println("         - NIC2 (" + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic2", config) +
		// ", " + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic2Bridge",
		// config) + ", " + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic2Mac", config)
		// + ")");
		// System.out.println("         - NIC3 (" + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic3", config) +
		// ", " + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic3Bridge",
		// config) + ", " + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic3Mac", config)
		// + ")");
		// System.out.println("         - NIC4 (" + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic4", config) +
		// ", " + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic4Bridge",
		// config) + ", " + //
		// InstanceParameterHelper.getInstanceParameter("vbox.nic4Mac", config)
		// + ")");
		// // Dump VPN config
		// if
		// (InstanceParameterHelper.getInstanceParameterAsBoolean("vpn.enabled",
		// config)) {
		// System.out.println("         - VPN (" + //
		// InstanceParameterHelper.getInstanceParameter("vpn.tap", config) +
		// ", " + //
		// InstanceParameterHelper.getInstanceParameter("vpn.name", config) +
		// ", " + //
		// InstanceParameterHelper.getInstanceParameter("vpn.description",
		// config) + ", " + //
		// InstanceParameterHelper.getInstanceParameter("vpn.hostname", config)
		// + ", " + //
		// InstanceParameterHelper.getInstanceParameter("vpn.port", config) +
		// ", " + //
		// InstanceParameterHelper.getInstanceParameter("vpn.clientKey", config)
		// + ", " + //
		// InstanceParameterHelper.getInstanceParameter("vpn.clientCert",
		// config) + ", " + //
		// InstanceParameterHelper.getInstanceParameter("vpn.caCert", config) +
		// ")");
		// }
		// long size =
		// InstanceParameterHelper.getInstanceParameter("vbox.disk1Size",
		// config);
		//
		//
		// System.out.println("Disk Size: "+size);
		// download instance VDI
		// FileDownloader down = new FileDownloader();
		// down.setDestination(new
		// FileOutputStream("/tmp/test-download-vdi.tmp"));
		// down.setRequest("localhost", 8080, instance.getUuid(), size);
		// down.startDownload();
		// while (down.getState() == State.DOWNLOADING || down.getState() ==
		// State.IDLE) {
		// Thread.sleep(500);
		// System.out.printf("wait... [%d%%]\n", (int) (down.getProgress() *
		// 100));
		// }
		System.out.println("done.");
	}
}
