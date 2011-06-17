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

package ch.admin.vbs.cube.client.wm.ui.tabs.action;

import java.awt.event.ActionEvent;

import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntry;

/**
 * 
 */
public class VmDetachUsbDevice extends VmAbstractAction {
	private static final long serialVersionUID = 0L;
	private final UsbDeviceEntry usb;

	/**
     * 
     */
	public VmDetachUsbDevice(VmHandle h, UsbDeviceEntry usb) {
		super(usb.getDevice().toString(), null, h);
		this.usb = usb;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.cube.client.wm.se.action.cube.CubeAbstractAction#exec(java
	 * .awt.event.ActionEvent)
	 */
	@Override
	public void exec(ActionEvent actionEvent) {
		for (IVmActionListener listener : getVmActionListeners()) {
			listener.detachUsbDevice(getVmHandle(), usb.getDevice());
		}
	}
}
