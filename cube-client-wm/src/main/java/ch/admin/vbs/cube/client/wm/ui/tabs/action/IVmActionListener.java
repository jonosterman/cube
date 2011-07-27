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

package ch.admin.vbs.cube.client.wm.ui.tabs.action;

import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.core.usb.UsbDevice;

/**
 * The listener interface for receiving virtual machine action events. The class
 * that is interested in processing a virtual machine action event implements
 * this interface.
 */
public interface IVmActionListener {
	/**
	 * Invokes that the virtual machine with the vmId should be started.
	 * 
	 * @param vmId
	 *            virtual machine to be started
	 */
	void startVm(VmHandle h);

	// /**
	// * Invokes that the virtual machine with the vmId should be stopped.
	// (Similar to unplug power line)
	// *
	// * @param vmId virtual machine to be stopped
	// */
	// void stopVm(String vmId);
	/**
	 * Invokes that the virtual machine with the vmId should be standbied.
	 * 
	 * @param vmId
	 *            virtual machine to be stopped
	 */
	void saveVm(VmHandle h);

	/**
	 * Invokes that the virtual machine with the vmId should be downloaded.
	 * 
	 * @param vmId
	 *            virtual machine to be downloaded
	 * @param location
	 *            the url of the location or null if server has to decide
	 */
	void stageVm(VmHandle h);

	/**
	 * Invokes that the virtual machine with the vmId should be shutdown.
	 * (Similar to press power off button)
	 * 
	 * @param vmId
	 *            virtual machine to be shutdown
	 */
	void poweroffVm(VmHandle h);

	void deleteVm(VmHandle h);

	void installGuestAdditions(VmHandle h);

	void detachUsbDevice(VmHandle vmHandle, UsbDevice usb);

	void attachUsbDevice(VmHandle vmHandle, UsbDevice usb);

	void setVmProperty(VmHandle vmHandle, String key, String value, boolean refreshAllVms);
}
