/**
 * Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package ch.admin.vbs.cube.core.vm.ctrtasks;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.core.I18nBundleProvider;
import ch.admin.vbs.cube.core.network.vpn.VpnManager;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmController;
import ch.admin.vbs.cube.core.vm.VmModel;
import ch.admin.vbs.cube.core.vm.VmStatus;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

public class Start extends AbstractCtrlTask {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(Start.class);

	public Start(VmController vmController, Map<String, VmStatus> tempStatus, IKeyring keyring, Vm vm, IContainerFactory containerFactory,
			VpnManager vpnManager, VBoxProduct product, Container transfer, VmModel vmModel) {
		super(vmController, tempStatus, keyring, vm, containerFactory, vpnManager, product, transfer, vmModel);
	}

	@Override
	public void run() {
		EncryptionKey vmKey = null;
		EncryptionKey rtKey = null;
		try {
			// set temporary status
			tempStatus.put(vm.getId(), VmStatus.STARTING);
			vm.setProgressMessage(I18nBundleProvider.getBundle().getString("vm.starting"));
			ctrl.refreshVmStatus(vm);
			//
			vmKey = keyring.getKey(vm.getVmContainer().getId());
			rtKey = keyring.getKey(vm.getRuntimeContainer().getId());
			containerFactory.mountContainer(vm.getVmContainer(), vmKey);
			containerFactory.mountContainer(vm.getRuntimeContainer(), rtKey);
			rtKey.shred(); // shred key asap
			vmKey.shred(); // shred key asap
			// prepare transfer folders
			File sessionTransferFolder = new File(transfer.getMountpoint(), vm.getId() + "_transfer");
			vm.setTempFolder(new File(sessionTransferFolder, "temporary"));
			vm.setImportFolder(new File(sessionTransferFolder, "import"));
			vm.setExportFolder(new File(sessionTransferFolder, "export"));
			vm.getTempFolder().mkdirs();
			vm.getImportFolder().mkdirs();
			vm.getExportFolder().mkdirs();
			//
			product.registerVm(vm); // register VM
			vpnManager.openVpn(vm, keyring); // open vpn
			product.startVm(vm); // start VM
		} catch (Exception e) {
			LOG.error("Failed to start VM", e);
		} finally {
			// shred keys
			if (rtKey != null)
				rtKey.shred(); // just in case..
			if (vmKey != null)
				vmKey.shred(); // just in case..
			// remove temporary status
			tempStatus.remove(vm.getId());
			ctrl.refreshVmStatus(vm);
		}
	}
}
