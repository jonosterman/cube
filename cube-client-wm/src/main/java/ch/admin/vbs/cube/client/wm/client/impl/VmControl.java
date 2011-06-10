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

package ch.admin.vbs.cube.client.wm.client.impl;

import ch.admin.vbs.cube.client.wm.client.ICubeClient;
import ch.admin.vbs.cube.client.wm.client.IVmControl;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.ui.IWindowsControl;

/**
 * @see IVmControl
 */
public class VmControl implements IVmControl {
	private ICubeClient client;
	private IWindowsControl winCtrl;

	@Override
	public void moveVm(VmHandle h, int monitor) {
		int oldMonitor = h.getMonitorIdx();
		// update handle
		h.setMonitorIdx(monitor);
		// trigger an event to update all NavigationBars
		client.notifyAllVmChanged();
		// move X window
		winCtrl.moveVmWindow(h, oldMonitor);
	}

	@Override
	public void showVm(VmHandle h) {
		winCtrl.showVmWindow(h);
	}

	@Override
	public void hideAllVms(int monitor) {
		winCtrl.hideAllVmWindows(monitor);
	}

	// #######################################################
	// Injections
	// #######################################################
	public void setupDependencies(ICubeClient client, IWindowsControl winCtrl) {
		this.client = client;
		this.winCtrl = winCtrl;
	}
}
