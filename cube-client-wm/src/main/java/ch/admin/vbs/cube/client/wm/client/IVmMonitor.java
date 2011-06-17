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

import ch.admin.vbs.cube.common.CubeClassification;
import ch.admin.vbs.cube.core.vm.VmStatus;

/**
 * This interface is used to fetch information about the VmHanlde list and VM's
 * details. It hides the 'Vm' object to UI elements. UI classes only interact
 * with VmHandle and IVmMonitor classes.
 * 
 * The goal is to avoid UI elements to retains references to VM objects.
 */
public interface IVmMonitor {
	VmStatus getVmState(VmHandle handle);

	String getVmDescription(VmHandle handle);

	String getVmDomain(VmHandle handle);

	String getVmName(VmHandle handle);

	int getVmProgress(VmHandle handle);

	String getVmProgressMessage(VmHandle handle);

	CubeClassification getVmClassification(VmHandle handle);

	String getVmProperty(VmHandle handle, String key);
}