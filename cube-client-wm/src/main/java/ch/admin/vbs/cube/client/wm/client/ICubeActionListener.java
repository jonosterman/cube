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

import ch.admin.vbs.cube.core.usb.UsbDevice;

/**
 * The listener interface for receiving cube action events. The class that is
 * interested in processing a cube action event implements this interface.
 */
public interface ICubeActionListener {
	/**
	 * Invokes the shutdown signal to shutdown the physical computer.
	 */
	void shutdownMachine();

	/**
	 * Invokes the cube to close the user session.
	 */
	void logoutUser();

	/**
	 * Invokes the cube to be locked.
	 */
	void lockCube();

	/**
	 * User enter its password
	 * @param requestId 
	 */
	void enteredPassword(char[] password, String requestId);

	/**
	 * User entered a confirmation
	 * @param requestId 
	 */
	void enteredConfirmation(int result, String requestId);

	void enteredUsbDevice(UsbDevice device, String requestId);
}
