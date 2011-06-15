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

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.core.ISession.IOption;
import ch.admin.vbs.cube.core.network.vpn.VpnManager;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmController;
import ch.admin.vbs.cube.core.vm.VmModel;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

public abstract class AbstractCtrlTask implements Runnable {
	protected EncryptionKey vmKey;
	protected EncryptionKey rtKey;
	protected final VmController ctrl;
	protected final IKeyring keyring;
	protected final Vm vm;
	protected final IContainerFactory containerFactory;
	protected final VpnManager vpnManager;
	protected final VBoxProduct product;
	protected final Container transfer;
	protected final VmModel vmModel;
	protected final IOption option;

	public AbstractCtrlTask(VmController vmController,  IKeyring keyring, Vm vm, IContainerFactory containerFactory,
			VpnManager vpnManager, VBoxProduct product, Container transfer, VmModel vmModel, IOption option) {
		this.ctrl = vmController;
		this.keyring = keyring;
		this.vm = vm;
		this.containerFactory = containerFactory;
		this.vpnManager = vpnManager;
		this.product = product;
		this.transfer = transfer;
		this.vmModel = vmModel;
		this.option = option;
	}
}
