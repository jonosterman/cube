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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.core.network.vpn.VpnManager;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmController;
import ch.admin.vbs.cube.core.vm.VmModel;
import ch.admin.vbs.cube.core.vm.VmStatus;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

public class Delete extends AbstractCtrlTask {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(Delete.class);

	public Delete(VmController vmController, Map<String, VmStatus> tempStatus, IKeyring keyring, Vm vm, IContainerFactory containerFactory,
			VpnManager vpnManager, VBoxProduct product, Container transfer, VmModel vmModel) {
		super(vmController, tempStatus, keyring, vm, containerFactory, vpnManager, product, transfer, vmModel);
	}

	@Override
	public void run() {
		try {
			containerFactory.deleteContainer(vm.getVmContainer());
			containerFactory.deleteContainer(vm.getRuntimeContainer());
			vmModel.fireVmUpdatedEvent(vm);
		} catch (Exception e) {
			LOG.error("Failed to delete VM", e);
		}
	}
}
