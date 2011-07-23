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

import java.util.List;

import ch.admin.vbs.cube.core.vm.Vm;

/**
 * 
 */
public interface ICubeClient {
	/**
	 * Get the VM represented by this handle.
	 * 
	 * @param handle
	 *            handle
	 * @return
	 */
	Vm getVm(VmHandle handle);

	/**
	 * Return an handle (place-holder) for the given VM.
	 * 
	 * @param vm
	 *            virtual machine
	 * @return
	 */
	VmHandle getVmHandle(Vm vm);

	/**
	 * Replace the old VM list with this one.
	 * 
	 * @param nvms
	 *            list of VMs
	 */
	void setVms(List<Vm> nvms);

	/**
	 * @return a list of all handles.
	 */
	List<VmHandle> listVms();

	/**
	 * Update VM in the local cache (which will notify the IVmMonitor and
	 * therefore all UI components)
	 * 
	 * @param vm
	 *            VM that changed
	 */
	void updateVm(Vm vm);

	void addListener(IVmChangeListener l);

	void removeListener(IVmChangeListener l);

	void notifyAllVmChanged();

	void notifyVmChanged(VmHandle h);
}