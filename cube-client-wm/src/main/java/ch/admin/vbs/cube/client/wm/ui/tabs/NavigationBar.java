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

package ch.admin.vbs.cube.client.wm.ui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.ICubeClient;
import ch.admin.vbs.cube.client.wm.client.IVmChangeListener;
import ch.admin.vbs.cube.client.wm.client.IVmControl;
import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.client.VmChangeEvent;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.client.VmHandleHumanComparator;
import ch.admin.vbs.cube.client.wm.ui.ICubeUI;
import ch.admin.vbs.cube.core.ICoreFacade;
import ch.admin.vbs.cube.core.network.INetworkManager;

import com.jidesoft.swing.JideTabbedPane;

/**
 * This frame contains tabs for one physical display.
 */
public class NavigationBar extends JFrame implements IVmChangeListener, INavigationBar {
	private static final long serialVersionUID = 1L;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(NavigationBar.class);
	public static final int FRAME_HEIGHT = 25;
	public static final String FRAME_TITLEPREFIX = "CubeNavigation#";
	private NavigationTabs tabs;
	private IVmMonitor vmMon;
	private Object lock = new Object();
	private HashMap<VmHandle, TabComponent> cmpMap = new HashMap<VmHandle, TabComponent>();
	private VmHandleHumanComparator cmp;
	private ICubeClient client;
	private final String monitorId;

	/** Constructor. */
	public NavigationBar(String monitorLabel) {
		this.monitorId = monitorLabel;
		initUI();
	}
	public void setIcon(String iconName) {
		tabs.setLogo(iconName);
	}

	// ################################################
	// ## Implements IVmChangeListener
	// ################################################
	@Override
	public void allVmsChanged() {
		// filter tabs for this display
		List<VmHandle> list = client.listVms();
		ArrayList<VmHandle> cDisplayList = new ArrayList<VmHandle>();
		for (VmHandle h : list) {
			if (monitorId.equals(h.getMonitorId())&& !"true".equalsIgnoreCase(vmMon.getVmProperty(h, "hidden"))) {
				cDisplayList.add(h);
			}
		}
		LOG.trace("Some VMs changed. Refresh tab list ({}/{})",cDisplayList.size(),list.size());
		refreshTabsVms(cDisplayList);
	}

	@Override
	public void vmChanged(VmChangeEvent event) {
		LOG.trace("VM [{}] changed", event.getVmHandle());
		refreshTab(event.getVmHandle());
	}

	// ################################################
	// ##
	// ################################################
	public NavigationTabs gettabs() {
		return tabs;
	}

	private void initUI() {
		getContentPane().setBackground(new Color(100, 100, 100));
		// configure frame
		setTitle(FRAME_TITLEPREFIX + monitorId);
		setUndecorated(true);
		setAlwaysOnTop(true);
		// add tabbed pane
		tabs = new NavigationTabs(monitorId);
		setLayout(new BorderLayout());
		add(tabs, BorderLayout.CENTER);
		// add placeholder to have at least one tab (or rendering of JTabs bugs.)
		tabs.addTab("", new TabComponent(null));
		tabs.setTabResizeMode(JideTabbedPane.RESIZE_MODE_COMPRESSED);
		
		pack();
	}

	public void refreshTabsVms(final List<VmHandle> vms) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// synchronize tab list with new VmHandle list
				synchronized (lock) {
					// index changes
					Collection<VmHandle> addedVms = addedVms(cmpMap.keySet(), vms);
					Collection<VmHandle> removedVms = removedVms(cmpMap.keySet(), vms);
					Collection<VmHandle> updatedVms = updatedVms(cmpMap.keySet(), vms);
					// rebuild map / tabs
					for (VmHandle h : addedVms) {
						TabComponent c = new TabComponent(h);
						// update map
						cmpMap.put(h, c);
						// add new tab
						tabs.insertTab(vmMon.getVmName(h), TabIconProvider.getStatusIcon(vmMon.getVmState(h),vmMon.getVpnState(h)), c, formatTooltip(h), getSortedIndex(h));
					}
					for (VmHandle h : updatedVms) {
						// update tab icon
						int idx = findTabComponentIndex(h);
						tabs.setTitleAt(idx, vmMon.getVmName(h));
						tabs.setIconAt(idx, TabIconProvider.getStatusIcon(vmMon.getVmState(h),vmMon.getVpnState(h)));
						tabs.setToolTipTextAt(idx, formatTooltip(h));
					}
					for (VmHandle h : removedVms) {
						// remove tab
						tabs.removeTabAt(findTabComponentIndex(h));
						// remove from map
						cmpMap.remove(h);
					}
					LOG.debug(String.format("Tabs updated add[%d] upd[%d] rem[%d]", addedVms.size(), updatedVms.size(), removedVms.size()));
					// if nothing is selected. select first
					if (tabs.getSelectedComponent() == null && cmpMap.size() > 0) {
						tabs.setSelectedIndex(0);
						LOG.debug("select first tab");
					} else {
						LOG.debug("keep selection [{}]", tabs.getSelectedIndex());
					}
					// if not tabs at all, add a placeholder to ensure
					// the main menu is always visible.
					if (tabs.getTabCount() == 0) {
						tabs.addTab("", new TabComponent(null));
						tabs.setTabResizeMode(JideTabbedPane.RESIZE_MODE_COMPRESSED);
					} else if (tabs.getTabCount() == 1 && tabs.getTabComponentAt(0) == null) {
						// let placeholder inplace
					} else {
						int ix = findTabComponentIndex(null);
						if (ix >= 0) {
							tabs.setTabResizeMode(JideTabbedPane.RESIZE_MODE_FIXED);
							tabs.remove(ix);
						}
					}
				}
			}

			private Collection<VmHandle> addedVms(Collection<VmHandle> oldlist, Collection<VmHandle> newlist) {
				ArrayList<VmHandle> result = new ArrayList<VmHandle>(newlist);
				result.removeAll(oldlist);
				return result;
			}

			private Collection<VmHandle> removedVms(Collection<VmHandle> oldlist, Collection<VmHandle> newlist) {
				ArrayList<VmHandle> result = new ArrayList<VmHandle>(oldlist);
				result.removeAll(newlist);
				return result;
			}

			private Collection<VmHandle> updatedVms(Collection<VmHandle> oldlist, Collection<VmHandle> newlist) {
				ArrayList<VmHandle> result = new ArrayList<VmHandle>(newlist);
				result.retainAll(oldlist);
				return result;
			}
		});
	}

	private int getSortedIndex(VmHandle h) {
		for (int i = 0; i < tabs.getTabCount(); ++i) {
			Component obj = tabs.getComponentAt(i);
			if (obj != null && obj instanceof TabComponent) {
				int x = cmp.compare(h, ((TabComponent) obj).getVmHandle());
				if (x <= 0) {
					return i;
				}
			}
		}
		return tabs.getTabCount();
	}

	public void refreshTab(final VmHandle h) {
		LOG.trace("Refresh Tab [{}]", h);
		if (h == null)
			throw new NullPointerException("Given VmHandle is null");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				int i = findTabComponentIndex(h);
				if (i >= 0) {
					// update icon
					tabs.setIconAt(i, TabIconProvider.getStatusIcon(vmMon.getVmState(h),vmMon.getVpnState(h)));
					// update tooltip
					tabs.setToolTipTextAt(i, formatTooltip(h));
				}
			}
		});
	}

	public void selectTab(final VmHandle h) {
		LOG.debug("Select Tab [{}]", h);
		if (h == null)
			throw new NullPointerException("Given VmHandle is null");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				int i = findTabComponentIndex(h);
				LOG.debug("handle found [{}]", i);
				if (i >= 0) {
					// select tab
					tabs.setSelectedIndex(i);
				}
			}
		});
	}

	private String formatTooltip(VmHandle h) {
		String message = vmMon.getVmProgressMessage(h);
		int p = vmMon.getVmProgress(h);
		String label = "";
		if (message == null || message.trim().length() == 0) {
			message = "";
		} else if (p<0) {
			label = "Progress:";
			message = String.format("%s", message);			
		} else {
			label = "Progress:";
			message = String.format("%s (%d%%)", message, p);
		}
		String tooltip = String
				.format("<html><table><tr><td>Name:</td><td>%s</td></tr><tr><td>Domain:</td><td>%s (%s)</td></tr><tr><td>State:</td><td>%s</td></tr><tr><td>Description:</td><td>%s</td></tr><tr>%s</td><td>%s</td></tr></table></html>", //
				vmMon.getVmName(h), //
						vmMon.getVmDomain(h), //
						vmMon.getVmClassification(h), //
						vmMon.getVmState(h), //
						vmMon.getVmDescription(h), //
						label, //
						message);
		return tooltip;
	}

	/** Injection 
	 * @param cubeUI 
	 * @param networkMgr */
	public void setup(IVmMonitor vmMon, IVmControl vmCtrl, ICoreFacade core, ICubeClient client, ICubeUI cubeUI, INetworkManager networkMgr) {
		this.vmMon = vmMon;
		client.addListener(this);
		this.client = client;
		cmp = new VmHandleHumanComparator(vmMon);
		tabs.setup(vmCtrl, vmMon, core, client, cubeUI, networkMgr);
	}

	private int findTabComponentIndex(VmHandle h) {
		for (int i = 0; i < tabs.getTabCount(); ++i) {
			Component obj = tabs.getComponentAt(i);
			if (obj != null && obj instanceof TabComponent) {
				TabComponent cmp = ((TabComponent) obj);
				if ((cmp.getVmHandle() == null && h == null) || (h != null && h.equals(cmp.getVmHandle()))) {
					return i;
				}
			}
		}
		return -1;
	}
}
