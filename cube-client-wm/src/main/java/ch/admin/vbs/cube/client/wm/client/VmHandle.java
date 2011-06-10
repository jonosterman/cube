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

package ch.admin.vbs.cube.client.wm.client;

/**
 * This class is used through the UI to reference a VM. It avoid UI element to
 * reference the vmId or Vm object directly.
 * 
 * This object does not hold VM specific attributes (like name, classification,
 * state, etc) since they could change at runtime (they will not be in sync with
 * values stored in Vm object) and may change in future release. Use IVmMonitor
 * to fetch those information.
 * 
 * This object may hold UI related attributes (monitorId, etc).
 * 
 * The mapping between the VmHandle and the Vm object is performed by CubeClient
 * class.
 */
public class VmHandle {
	private final String vmId;
	private int monitorIdx;

	public VmHandle(String vmId) {
		if (vmId == null)
			throw new NullPointerException("vmId should not be null");
		this.vmId = vmId;
	}

	public String getVmId() {
		return vmId;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof VmHandle && vmId.equals(((VmHandle) obj).vmId);
	}

	@Override
	public int hashCode() {
		return vmId.hashCode();
	}

	public int getMonitorIdx() {
		return monitorIdx;
	}

	public void setMonitorIdx(int monitorIdx) {
		this.monitorIdx = monitorIdx;
	}

	@Override
	public String toString() {
		return String.format("VmHandle[%s][%d]", vmId, monitorIdx);
	}
}
