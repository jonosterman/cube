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
 * This interface allows UI elements to control X Windows (raise a VM on top of
 * others, hide VMs, etc)
 */
public interface IVmControl {
	/**
	 * Request to display the given VM.
	 * 
	 * @param h
	 *            VM to be raised.
	 */
	void showVm(VmHandle h);

	/**
	 * Request to hide all VMs on the given monitor.
	 * 
	 * @param monitor
	 */
	void hideAllVms(int monitor);

	/**
	 * Request to move the given VM on the target monitor. If the move is
	 * effective, the requesting class will be notified through the
	 * IVmChangeListener interface.
	 * 
	 * @param h
	 *            VM handle
	 * @param targetMonitor
	 *            target monitor.
	 */
	void moveVm(VmHandle h, int targetMonitor);
}
