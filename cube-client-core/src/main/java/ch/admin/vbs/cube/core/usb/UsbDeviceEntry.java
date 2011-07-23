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

package ch.admin.vbs.cube.core.usb;

public class UsbDeviceEntry {
	
	private final DeviceEntryState state;
	private final String vmId;
	private final UsbDevice device;

	public enum DeviceEntryState { AVAILABLE, ALREADY_ATTACHED, ATTACHED_TO_ANOTHER_VM }

	public UsbDeviceEntry(String vmId, UsbDevice device, DeviceEntryState state) {
		this.vmId = vmId;
		this.device = device;
		this.state = state;
	}

	public DeviceEntryState getState() {
		return state;
	}

	public String getVmId() {
		return vmId;
	}

	public UsbDevice getDevice() {
		return device;
	}
	
	@Override
	public String toString() {
		return device.toString();
	}
	
}
