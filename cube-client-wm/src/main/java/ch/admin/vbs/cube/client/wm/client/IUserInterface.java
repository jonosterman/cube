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

package ch.admin.vbs.cube.client.wm.client;

import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntryList;

/**
 * This interface is used to control the UI: if it should show a specific dialog
 * or the VMs (NavigationTabs and VMs). This interface is NOT used to update tab
 * content (use IVmChangeListener).
 */
public interface IUserInterface {
	/**
	 * Display message dialog.
	 * 
	 * @param message
	 * @param options
	 */
	void showMessageDialog(String message, int options);

	/**
	 * Display PIN (Password) dialog.
	 * 
	 * @param additionalMessage
	 * @param requestId 
	 */
	void showPinDialog(String additionalMessage, String requestId);

	/**
	 * Show file transfer dialog
	 * 
	 * @param vm
	 * @param file
	 */
	void showTransferDialog(VmHandle h, RelativeFile file);

	/**
	 * Show VMs and navigation bars
	 */
	void showVms();

	/**
	 * Show confirmation dialog
	 * @param requestId 
	 */
	void showConfirmationDialog(String messageKey, String requestId);

	/**
	 * Display a dialog where user could pick an already connected USB device to
	 * bind it to the selected VM.
	 * @param list 
	 * 
	 * @param h
	 *            target VM.
	 */
	void showUsbDeviceDialog(String messageKey, UsbDeviceEntryList list, String requestId);

	/** close current dialog. */
	void closeDialog();
}
