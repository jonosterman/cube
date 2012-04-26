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

package ch.admin.vbs.cube.client.wm.client.impl;

import ch.admin.vbs.cube.client.wm.client.ICubeClient;
import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.common.CubeClassification;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmNetworkState;
import ch.admin.vbs.cube.core.vm.VmState;
import ch.admin.vbs.cube.core.vm.VmVpnState;

/**
 * @see IVmMonitor
 * @see ICubeClient
 */
public class VmMonitor implements IVmMonitor {
	private ICubeClient client;

	@Override
	public CubeClassification getVmClassification(VmHandle handle) {
		Vm vm = client.getVm(handle);
		return vm == null ? null : vm.getDescriptor().getRemoteCfg().getClassification();
	}

	@Override
	public String getVmDescription(VmHandle handle) {
		Vm vm = client.getVm(handle);
		return vm == null ? "unknown" : vm.getDescriptor().getRemoteCfg().getName();
	}

	@Override
	public String getVmDomain(VmHandle handle) {
		Vm vm = client.getVm(handle);
		return vm == null ? "unknown" : vm.getDescriptor().getRemoteCfg().getDomain();
	}

	@Override
	public String getVmName(VmHandle handle) {
		Vm vm = client.getVm(handle);
		return vm == null ? "unknown" : vm.getDescriptor().getRemoteCfg().getName();
	}

	@Override
	public VmState getVmState(VmHandle handle) {
		Vm vm = client.getVm(handle);
		return vm == null ? VmState.UNKNOWN : vm.getVmState();
	}
	@Override
	public VmVpnState getVpnState(VmHandle handle) {
		Vm vm = client.getVm(handle);
		return vm == null ? VmVpnState.NOT_CONNECTED: vm.getVpnState();
	}
	
	@Override
	public VmNetworkState getVmNetworkState(VmHandle handle) {
		Vm vm = client.getVm(handle);
		return vm == null ? VmNetworkState.CUBE: vm.getNetworkState();
	}

	@Override
	public int getVmProgress(VmHandle handle) {
		Vm vm = client.getVm(handle);
		return vm == null ? 0 : vm.getProgress();
	}

	@Override
	public String getVmProgressMessage(VmHandle handle) {
		Vm vm = client.getVm(handle);
		return vm == null ? "" : vm.getProgressMessage();
	}
	
	@Override
	public String getVmProperty(VmHandle handle, String key) {
		Vm vm = client.getVm(handle);
		return vm == null ? "" : vm.getDescriptor().getLocalCfg().getProperty(key);
	}

	// #######################################################
	// Injection
	// #######################################################
	public void setup(ICubeClient client) {
		this.client = client;
	}
}
