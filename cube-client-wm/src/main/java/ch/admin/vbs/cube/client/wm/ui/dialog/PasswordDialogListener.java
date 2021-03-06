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

package ch.admin.vbs.cube.client.wm.ui.dialog;

/**
 * The listener interface for receiving the close event from the
 * {@link PasswordDialog}. The class that is interested in the close event with
 * the password implements this interface.
 * 
 * 
 */
public interface PasswordDialogListener {
	/**
	 * Notify the listener that the {@link PasswordDialog} has been closed and
	 * send the password.
	 * 
	 * @param password
	 *            the entered password when the user pressed ok/login, otherwise
	 *            null
	 */
	void quit(char[] password);
}
