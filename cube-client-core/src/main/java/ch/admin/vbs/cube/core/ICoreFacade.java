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
package ch.admin.vbs.cube.core;

import java.net.URL;

import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.usb.UsbDevice;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntryList;

/**
 * The core facade is the interface between the window manager and the core
 * system. Works together with IWindowManagerFacade as a bridge.
 * 
 * @see bridge pattern
 */
public interface ICoreFacade {
	/**
	 * This method should be called from the WindowManager, when the user
	 * type-in the password to open the key store.
	 * 
	 * @See WindowManager#showGetPIN()
	 * @param password
	 *            the password for the key store
	 */
	void enteredPassword(char[] password, String requestId);

	void enteredConfirmation(int result, String requestId);

	/**
	 * Starts the virtual machine with the given id, if the user is allowed to.
	 * 
	 * @param vmId
	 *            the id of the virtual machine
	 */
	void startVm(String vmId);

	/**
	 * Standby the virtual machine with the given id, if the user is allowed to.
	 * 
	 * @param vmId
	 *            the id of the virtual machine
	 */
	void standByVm(String vmId);

	/**
	 * Powers off the virtual machine with the given id, if the user is allowed
	 * to. (unplug power line)
	 * 
	 * @param vmId
	 *            the id of the virtual machine
	 */
	void powerOffVm(String vmId);

	/**
	 * Initiates the stager to stage the virtual machine. The location must be
	 * null, if the vm should be downloaded from the default server. If the
	 * location is a file URL, the stager will start copy the file.
	 * 
	 * @param vmId
	 *            the id of the virtual machine
	 * @param location
	 *            the exact URL of the virtual machine or null for downloading
	 *            the vm from the default server
	 */
	void stageVm(String vmId, URL location);

	/**
	 * Invokes the core to lock the whole cube (screensaver).
	 */
	void lockCube();

	/**
	 * Invokes the core to logout the current user.
	 */
	void logoutUser();

	/**
	 * Invokes the core to shutdown the physical machine.
	 */
	void shutdownMachine();

	/**
	 * Transfers a file or a directory from the export folder of a source
	 * virtual machine to the import folder of a target virtual machine.
	 * 
	 * @param fileName
	 *            name of the exported file or directory.
	 * @param vmIdFrom
	 *            source virtual machine.
	 * @param vmIdTo
	 *            target virtual machine.
	 */
	void fileTransfer(RelativeFile fileName, String vmIdFrom, String vmIdTo);

	/**
	 * Deletes all the files and folder from the export folder of the given
	 * virtual machine.
	 * 
	 * @param vmId
	 *            virtual machine whose export folder is cleaned up.
	 */
	void cleanUpExportFolder(String vmId);

	/**
	 * Delete VM.
	 * 
	 * @param vmId
	 */
	void deleteVm(String vmId);

	/**
	 * Mount guest addition CD into VM
	 * 
	 * @param vmId
	 */
	void installGuestAdditions(String vmId);

	// ==========================================
	// USB stuff
	// ==========================================
	void attachUsbDevice(String vmId, UsbDevice usbDevice);

	void detachUsbDevice(String vmId, UsbDevice usbDevice);

	void enteredUsbDevice(UsbDevice device, String requestId);

	void attachUsbDeviceRequest(String vmId);

	UsbDeviceEntryList getUsbDeviceList(String vmId);

	// ==========================================
	// Properties
	// ==========================================
	void setVmProperty(String vmId, String key, String value, boolean refreshAllVms);

}
