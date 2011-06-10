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

package ch.admin.vbs.cube.core.old;

import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.core.usb.UsbDevice;
import ch.admin.vbs.cube.core.usb.UsbManager;

/**
 * This demo program use UsbManager class to retrieve a list of connected USB
 * devices and prints them.
 * 
 */
public class DemoUsbMonitor {
	public static void main(String[] args) throws CubeException {
		UsbManager m = new UsbManager();
		for (UsbDevice d : m.listDevices()) {
			System.out.println(d);
		}
	}
}
