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

import ch.admin.vbs.cube.core.impl.CallbackPin;

/**
 * ILogin UI is used to display login related dialogs. It has priority over all
 * other UI elements (lock screen)
 * 
 * @see ILogin
 */
public interface ILoginUI {
	/** Dialogs types */
	public enum LoginDialogType {
		NO_OPTION, SHUTDOW_OPTION
	}

	/**
	 * Display a 'Enter PIN' dialog with a user message (example:
	 * "Login failed, try again"). And hide VMs.
	 */
	void showPinDialog(String message, CallbackPin callback);

	/** Display message dialog and hide VMs. */
	void showDialog(String message, LoginDialogType type);

	/** close any opened dialog and show current session VMs */
	void closeDialog();
	
}
