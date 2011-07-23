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

package ch.admin.vbs.cube.core.vm.ctrtasks;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.core.I18nBundleProvider;
import ch.admin.vbs.cube.core.ISession.IOption;
import ch.admin.vbs.cube.core.network.vpn.VpnManager;
import ch.admin.vbs.cube.core.vm.IVmProduct.VmProductState;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmController;
import ch.admin.vbs.cube.core.vm.VmModel;
import ch.admin.vbs.cube.core.vm.VmStatus;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

public class Start extends AbstractCtrlTask {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(Start.class);
	private static final long START_TIMEOUT = 40000; // ms
	private static final long UNKNOWN_STATE_TIMEOUT = 4000; // ms

	public Start(VmController vmController, IKeyring keyring, Vm vm, IContainerFactory containerFactory, VpnManager vpnManager, VBoxProduct product,
			Container transfer, VmModel vmModel, IOption option) {
		super(vmController, keyring, vm, containerFactory, vpnManager, product, transfer, vmModel, option);
	}

	@Override
	public void run() {
		EncryptionKey vmKey = null;
		EncryptionKey rtKey = null;
		try {
			// set temporary status
			ctrl.setTempStatus(vm, VmStatus.STARTING);
			//
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
			/*
			 * wait that the VM reach the state RUNNING. If it fails within the
			 * timeout, remove the tempStatus flag and let VmContorller handle
			 * it.
			 */
			boolean isUnknown = false;
			long isUnknownSince = 0;
			long timeout = System.currentTimeMillis() + START_TIMEOUT;
			while (System.currentTimeMillis() < timeout) {
				VmProductState ps = product.getProductState(vm);
				if (ps == VmProductState.RUNNING || ps == VmProductState.ERROR) {
					break;
				} else if (ps == VmProductState.UNKNOWN) {
					if (isUnknown) {
						// was already unknown. check timeout
						if (isUnknownSince + UNKNOWN_STATE_TIMEOUT < System.currentTimeMillis()) {
							/*
							 * VM was too long in UNKNOWN state (VirtualBox vm
							 * is 'saved' or 'stopped' which is normal between
							 * the 'register' and 'start' commands or between
							 * 'save/poweroff' and 'unregister' commands) but
							 * which should last only few seconds. Do not wait
							 * until START_TIMEOUT is reached and break asap.
							 */
							LOG.debug("VM was in UNKNOWN state for to many seconds. It is abnormal.");
							break;
						}
					} else {
						// just entered unknown state
						isUnknown = true;
						isUnknownSince = System.currentTimeMillis();
					}
				} else {
					isUnknown = false;
				}
				LOG.debug("Wait VM to be RUNNING..");
				Thread.sleep(500);
			}
			LOG.debug("Wait VM reached state [{}]", product.getProductState(vm));
		} catch (Exception e) {
			LOG.error("Failed to start VM", e);
		} finally {
			// shred keys
			if (rtKey != null)
				rtKey.shred(); // just in case..
			if (vmKey != null)
				vmKey.shred(); // just in case..
			ctrl.clearTempStatus(vm);
			ctrl.refreshVmStatus(vm);
		}
	}
}
