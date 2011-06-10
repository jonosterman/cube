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

import org.virtualbox_4_0.IMachine;
import org.virtualbox_4_0.ISession;
import org.virtualbox_4_0.IUSBDeviceFilter;
import org.virtualbox_4_0.IVirtualBox;
import org.virtualbox_4_0.LockType;
import org.virtualbox_4_0.VirtualBoxManager;

/**
 * This demo try to connect a usb device to a running VM.
 * 
 */
public class DemoLiveUsbConnect {
	public static final String VM_NAME = "OLD Gaia";
	public static final String USB_VENDORID = "13fe";
	public static final String USB_PRODUCTID = "1e00";

	public static void main(String[] args) {
		DemoLiveUsbConnect v = new DemoLiveUsbConnect();
		v.start();
	}

	private void start() {
		VirtualBoxManager mgr = VirtualBoxManager.createInstance(null);
		mgr.connect("http://localhost:18083", "", "");
		IVirtualBox vbox = mgr.getVBox();
		//
		IMachine machine = vbox.findMachine(VM_NAME);
		ISession session = mgr.getSessionObject();
		machine.lockMachine(session, LockType.Shared);
		machine = session.getMachine();
		try {
			IUSBDeviceFilter filter = machine.getUSBController().createDeviceFilter("blah blah");
			filter.setActive(true);
			filter.setVendorId(USB_VENDORID);
			filter.setProductId(USB_PRODUCTID);
			machine.getUSBController().insertDeviceFilter(0L, filter);
			System.out.println("USB filter added. Saving settings.");
			machine.saveSettings();
		} finally {
			session.unlockMachine();
		}
	}
}
