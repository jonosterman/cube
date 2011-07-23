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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.ICubeClient;
import ch.admin.vbs.cube.client.wm.client.IVmChangeListener;
import ch.admin.vbs.cube.client.wm.client.VmChangeEvent;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.ui.ICubeUI;
import ch.admin.vbs.cube.core.vm.Vm;

/**
 * This class bridge the gap between the Core (IClientCore) and UI elements
 * (NavigationBar, WindowsManager, etc.)
 * 
 * This class provides two things: <br>
 * - Implements the IClientFacade (part of IClientFacade/ICoreFacade bridge
 * pattern)<br>
 * - Implements IClientControl, IVmControl, IVmMonitor to which are used by UI
 * elements to control VMs<br>
 * It communicates back to UI elements through: <br>
 * - IVmChangeListener (notify that tabs should be refreshed)<br>
 * - IUserInterface (show dialogs when requested through IClientFacade)<br>
 * 
 * All these functionalities have been split in several classes, so CubeClient
 * does not really 'implements' any interfaces.
 * 
 * 
 * 
 */
public class CubeClient implements ICubeClient {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(CubeClient.class);
	private HashMap<VmHandle, Vm> vmsStr = new HashMap<VmHandle, Vm>();
	private HashMap<String, VmHandle> vmsRev = new HashMap<String, VmHandle>();
	private ArrayList<IVmChangeListener> listeners = new ArrayList<IVmChangeListener>();
	private ICubeUI cubeUI;

	public CubeClient() {
	}

	@Override
	public List<VmHandle> listVms() {
		synchronized (vmsStr) {
			return new ArrayList<VmHandle>(vmsStr.keySet());
		}
	}

	@Override
	public void setVms(List<Vm> nvms) {
		synchronized (vmsStr) {
			// synchronize VM list with VmHandle list
			// Collection<Vm> removedVms = removedVms(vmsStr.values(),nvms);
			Collection<Vm> addedVms = addedVms(vmsStr.values(), nvms);
			Collection<Vm> updatedVms = updatedVms(vmsStr.values(), nvms);
			// rebuild VM/VmHandle maps
			HashMap<VmHandle, Vm> ncol1 = new HashMap<VmHandle, Vm>();
			HashMap<String, VmHandle> ncol2 = new HashMap<String, VmHandle>();
			for (Vm v : updatedVms) {
				VmHandle h = vmsRev.get(v.getId());
				ncol1.put(h, v);
				ncol2.put(v.getId(), h);
			}
			for (Vm v : addedVms) {
				VmHandle h = new VmHandle(v.getId(), cubeUI.getDefaultScreen().getId());
				ncol1.put(h, v);
				ncol2.put(v.getId(), h);
			}
			// replace old maps
			vmsStr = ncol1;
			vmsRev = ncol2;
		}
		notifyAllVmChanged();
	}

	@Override
	public VmHandle getVmHandle(Vm vm) {
		synchronized (vmsStr) {
			VmHandle h = vmsRev.get(vm.getId());
			if (h == null) {
				// DEBUG
				LOG.debug("no handle found for vm [{}]", vm.getId());
				for (Entry<String, VmHandle> e : vmsRev.entrySet()) {
					LOG.debug("   -- [{}][{}]", e.getKey(), e.getValue());
				}
				throw new NullPointerException("No matching handle found for VM [" + vm + "]");
			}
			return h;
		}
	}

	@Override
	public Vm getVm(VmHandle handle) {
		synchronized (vmsStr) {
			return vmsStr.get(handle);
		}
	}

	@Override
	public void updateVm(Vm vm) {
		VmHandle h = null;
		synchronized (vmsStr) {
			h = vmsRev.get(vm.getId());
			// place new VM in cache
			vmsStr.put(h, vm);
		}
		// notify UI for changes
		notifyVmChanged(h);
	}
	
	// #######################################################
	// Events methods
	// #######################################################
	@Override
	public void notifyAllVmChanged() {
		synchronized (listeners) {
			for (IVmChangeListener l : listeners) {
				l.allVmsChanged();
			}
		}
	}

	@Override
	public void notifyVmChanged(VmHandle h) {
		synchronized (listeners) {
			for (IVmChangeListener l : listeners) {
				l.vmChanged(new VmChangeEvent(h));
			}
		}
	}

	@Override
	public void addListener(IVmChangeListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	@Override
	public void removeListener(IVmChangeListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}
	
	public void setup(ICubeUI cubeUI) {
		this.cubeUI = cubeUI;
	}

	// #######################################################
	// Private methods
	// #######################################################
	/**
	 * Return the VMs that are in the new list and not in the old list.
	 */
	private Collection<Vm> addedVms(Collection<Vm> oldlist, Collection<Vm> newlist) {
		ArrayList<Vm> res = new ArrayList<Vm>(newlist);
		res.removeAll(oldlist);
		return res;
	}

	/**
	 * Return the VMs that are in both new and old list.
	 */
	private Collection<Vm> updatedVms(Collection<Vm> oldlist, Collection<Vm> newlist) {
		ArrayList<Vm> res = new ArrayList<Vm>(newlist);
		res.retainAll(oldlist);
		return res;
	}
}
