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

package ch.admin.vbs.cube.client.wm.ui.tabs;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.ICubeClient;
import ch.admin.vbs.cube.client.wm.client.IVmChangeListener;
import ch.admin.vbs.cube.client.wm.client.IVmControl;
import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.client.VmChangeEvent;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.core.ICoreFacade;

public class NavigationBar implements IVmChangeListener, INavigationBar {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(NavigationBar.class);
	// IoC
	private ICubeClient client;
	// UI
	private NavigationFrame navFrame;
	private final int monitorIdx;
	private IVmMonitor vmMon;

	public NavigationBar(int monitorIdx, int monitorCount, JFrame refFrame) {
		this.monitorIdx = monitorIdx;
		navFrame = new NavigationFrame(monitorIdx, monitorCount, refFrame);
	}

	// ################################################
	// ## Implements IVmChangeListener
	// ################################################
	@Override
	public void allVmsChanged() {
		LOG.debug("VMs changed");
		// filter tabs for this display
		List<VmHandle> list = client.listVms();
		ArrayList<VmHandle> cDisplayList = new ArrayList<VmHandle>();
		for (VmHandle h : list) {
			if (h.getMonitorIdx() == monitorIdx && !"true".equalsIgnoreCase(vmMon.getVmProperty(h, "hidden"))) {
				cDisplayList.add(h);
			}
		}
		navFrame.refreshTabsVms(cDisplayList);
	}

	@Override
	public void vmChanged(VmChangeEvent event) {
		LOG.debug("VM [{}] changed", event.getVmHandle());
		navFrame.refreshTab(event.getVmHandle());
	}

	// ################################################
	// ## Getter/Setter Methods
	// ################################################
	public NavigationFrame getNavBar() {
		return navFrame;
	}

	// ################################################
	// ## Injections
	// ################################################
	public void setup(ICubeClient client, ICoreFacade core, IVmControl vmCtrl, IVmMonitor vmMon) {
		this.client = client;
		this.vmMon = vmMon;
		navFrame.setup(vmMon, vmCtrl, core, client);
	}
}
