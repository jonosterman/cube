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

package ch.admin.vbs.cube.core;

import java.util.List;

import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.ISession.ISessionStateDTO;
import ch.admin.vbs.cube.core.vm.Vm;

/**
 * Interface for UI component. Works with the ICoreFacade as a bridge.
 * 
 * IClientFacade describe a simple UI that may display user's VMs or a dialog.
 * Always only one dialog is visible and this dialog hide the VMs content.
 * 
 * All methods are not-blocking. Blocking method would problematic, in case the
 * user removes its token (we must break the method call). Therefore methods
 * that should return a value must use ICoreFacade.
 * 
 * @see bridge pattern
 */
public interface IClientFacade {
	/** no option */
	public static int OPTION_NONE = 0;
	/** add shutdown menu to dialog */
	public static int OPTION_SHUTDOWN = 1 << 1;

	/**
	 * Forces the WindowManager do refresh his tab list with the new list. This
	 * will close any opened dialog (locked user, PIN, etc)
	 * 
	 * @param vmList
	 *            list of VM to display in the navigation bar
	 */
	void displayTabs(List<Vm> vmList);

	/**
	 * Refresh progress/status of the given VM
	 * 
	 * @param vm
	 *            the updated VM
	 */
	void notifiyVmUpdated(Vm vm);

	/**
	 * The implementor of this method has to make sure that a password is
	 * retrieved and {@link ICubeCore#enteredPassword(String)} MUST be called
	 * afterwards! EVEN IF NO PASSWORD HAS BEEN RETRIEVED!
	 * 
	 * @param additionalMessage
	 *            message shown in the password dialog along with the standard
	 *            message.
	 * @param string
	 */
	void showGetPIN(String additionalMessage, String requestId);

	/**
	 * Shows a message dialog with the given message.
	 * 
	 * @param message
	 *            the message to be shown
	 * @param option
	 *            IClientFacade.OPTION_SHUTDOWN if you want to display the
	 *            shutdown option in the dialog. It is important to allow the
	 *            user to shutdown the machine even if someone have running VM
	 *            in background to avoid the user to perform a brutal power-off.
	 */
	void showMessage(String message, int option);

	/**
	 * Requests a file transfer wizard to appear.
	 * 
	 * @param vm
	 *            the virtual machine from which the file is being exported.
	 * @param fileName
	 *            the name of the file being exported.
	 */
	void showTransferWizard(Vm vm, RelativeFile fileName);

	/**
	 * Request a user confirmation
	 * 
	 * @param messageKey
	 * @param string
	 * @return 1 = confirmed, 0 = cancel
	 */
	void askConfirmation(String messageKey, String requestId);

	void notifySessionStateUpdate(ISessionStateDTO state);
}
