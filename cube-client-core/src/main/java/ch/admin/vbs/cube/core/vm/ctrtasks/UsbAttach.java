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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.ISession.IOption;
import ch.admin.vbs.cube.core.usb.UsbDevice;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

public class UsbAttach implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(UsbAttach.class);
	private final VBoxProduct product;
	private final IOption option;
	private final Vm vm;

	public UsbAttach(Vm vm, VBoxProduct product, IOption option) {
		this.vm = vm;
		this.product = product;
		this.option = option;
	}

	@Override
	public void run() {
		try {
			product.attachUsb(vm, (UsbDevice) option);
		} catch (Exception e) {
			LOG.error("Failed to connect USB device", e);
		}
	}
}
