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

package ch.admin.vbs.cube.client.wm.ui;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.ICubeClient;
import ch.admin.vbs.cube.client.wm.client.IUserInterface;
import ch.admin.vbs.cube.client.wm.client.IVmControl;
import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.ui.tabs.NavigationBar;
import ch.admin.vbs.cube.client.wm.ui.wm.BackgroundFrame;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;
import ch.admin.vbs.cube.client.wm.xrandx.impl.AcpiListener;
import ch.admin.vbs.cube.client.wm.xrandx.impl.AcpiListener.AcpiEvent;
import ch.admin.vbs.cube.client.wm.xrandx.impl.AcpiListener.IAcpiEventListener;
import ch.admin.vbs.cube.client.wm.xrandx.impl.XrandrTwoDisplayLayout;
import ch.admin.vbs.cube.client.wm.xrandx.impl.XrandrTwoDisplayLayout.Layout;
import ch.admin.vbs.cube.core.ICoreFacade;
import ch.admin.vbs.cube.core.network.INetworkManager;

public class CubeUI implements ICubeUI/* , IXRListener */{
	private static final Logger LOG = LoggerFactory.getLogger(CubeUI.class);
	private static CubeScreen last;
	private IXrandr xrandr;
	private Object lock = new Object();
	private HashMap<String, CubeScreen> cubeScrs = new HashMap<String, CubeUI.CubeScreen>();
	private ICoreFacade core;
	private ICubeClient client;
	private IVmMonitor vmMonitor;
	private IVmControl vmControl;
	private ICubeUI cubeUI;
	private INetworkManager networkMgr;
	private XrandrTwoDisplayLayout layoutMgr;
	private AcpiListener acpi;
	private Layout currentLayout = Layout.AB;
	private IUserInterface userIface;
	private boolean started;

	public void setup(ICoreFacade core, ICubeClient client, IXrandr xrandr, IVmMonitor vmMonitor, IVmControl vmControl, ICubeUI cubeUI, IUserInterface userIface, INetworkManager networkMgr) {
		this.core = core;
		this.client = client;
		this.vmMonitor = vmMonitor;
		this.vmControl = vmControl;
		this.cubeUI = cubeUI;
		this.xrandr = xrandr;
		this.userIface = userIface;
		this.networkMgr = networkMgr;
		// this.xrandr.addListener(this);
		layoutMgr = new XrandrTwoDisplayLayout();
		// force first sync
		// screenChanged();
	}

	public void start() {
		// force first layout
		layoutScreens(Layout.AB);
		// init acpi listener (for acpi buttons + lid)
		acpi = new AcpiListener();
		acpi.addListener(new IAcpiEventListener() {
			@Override
			public void acpi(AcpiEvent e) {
				switch (e.getType()) {
				case EXTERN_DISPLAY_BUTTON:
					// If user press the 'screen switch' special button, we just
					// switch to the next layout.
					int idx = 0;
					if (currentLayout != null) {
						for (int i = 0; i < Layout.values().length; i++) {
							if (Layout.values()[i] == currentLayout) {
								idx = i;
								break;
							}
						}
					}
					// loop around possible layouts
					idx = (idx + 1) % Layout.values().length;
					// re-layout screens based on new layout mode
					layoutScreens(Layout.values()[idx]);
					break;
				case LID_EVENT:
					// re-layout screens based on new screen state
					layoutScreens(currentLayout);
					break;
				default:
					break;
				}
			}
		});
		acpi.start();
		started = true;
	}

	@Override
	public void layoutScreens(Layout layout) {
		// update reference to last selected layout
		currentLayout = layout;
		// layout screens (using xrandx)
		layoutMgr.layout(layout, xrandr);
		// sync CubeScreen dimensions
		updateCubeScreen(xrandr.getScreens());
		// remap tabs in not-active screens
		for (CubeScreen c : cubeScrs.values()) {
			if (!c.active) {
				c.moveAllTabsToAnotherScreen();
			}
		}
		if (started) {
		userIface.refresh();
		}
	}

	@Override
	public CubeScreen getScreen(String monitorId) {
		synchronized (lock) {
			return cubeScrs.get(monitorId);
		}
	}

	@Override
	public CubeScreen getDefaultScreen() {
		synchronized (lock) {
			for (CubeScreen c : cubeScrs.values()) {
				if (c.isActive()) {
					last = c;
					return c;
				}
			}
			LOG.error("No screen to return as default");
			return null;
		}
	}

	public static Rectangle getDefaultScreenBounds() {
		if (last == null) {
			return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
		} else {
			return new Rectangle(last.bgdBounds);
		}
	}

	@Override
	public List<CubeScreen> getScreens() {
		synchronized (lock) {
			ArrayList<CubeScreen> list = new ArrayList<CubeUI.CubeScreen>();
			for (CubeScreen c : cubeScrs.values()) {
				list.add(c);
			}
			return list;
		}
	}

	private void updateCubeScreen(List<XRScreen> screens) {
		synchronized (lock) {
			@SuppressWarnings("unchecked")
			HashMap<String, CubeScreen> copy = (HashMap<String, CubeScreen>) cubeScrs.clone();
			for (XRScreen s : screens) {
				CubeScreen c = copy.remove(s.getId());
				if (c == null) {
					// Create new CubeScreen object
					c = new CubeScreen();
					cubeScrs.put(s.getId(), c);
					c.init(s);
				} else {
					// Update existing CubeScreen object
					c.update(s);
				}
			}
			// removed screens
			for (CubeScreen c : copy.values()) {
				// Dispose CubeScreen object
				c.dispose();
				cubeScrs.remove(c.id);
			}
		}
	}

	/**
	 * CubeScreen hold a reference to the background frame, and the
	 * NavigationBar's frame
	 */
	public class CubeScreen/* implements ComponentListener */{
		private JFrame backgroundFrame;
		private String id; // screen ID (VGA1, LVDS1, ...)
		private NavigationBar navbar;
		private boolean active; // plugged-in and configured
		private Rectangle bgdBounds;
		private Rectangle navBounds;

		public CubeScreen() {
		}

		public void init(XRScreen screen) {
			id = screen.getId();
			active = screen.getState() == State.CONNECTED_AND_ACTIVE;
			LOG.debug(
					"Create CubeScreen[{}] with state [" + screen.getState() + "] and position (" + screen.getPosX() + ":" + screen.getPosY() + ")("
							+ screen.getCurrentWidth() + "x" + screen.getCurrentHeight() + ")", id);
			// create background frame
			bgdBounds = new Rectangle(screen.getPosX(), screen.getPosY(), screen.getCurrentWidth(), screen.getCurrentHeight());
			backgroundFrame = new BackgroundFrame(this.id, bgdBounds);
			backgroundFrame.setVisible(active);
			// create navigation bar
			navBounds = new Rectangle(screen.getPosX(), screen.getPosY(), screen.getCurrentWidth(), NavigationBar.FRAME_HEIGHT);
			navbar = new NavigationBar(screen.getId());
			navbar.setBounds(navBounds);
			navbar.setup(vmMonitor, vmControl, core, client, cubeUI, networkMgr);
			navbar.setVisible(active);
		}

		/** Take care of size and state changes. */
		public void update(XRScreen screen) {
			bgdBounds = new Rectangle(screen.getPosX(), screen.getPosY(), screen.getCurrentWidth(), screen.getCurrentHeight());
			navBounds = new Rectangle(screen.getPosX(), screen.getPosY(), screen.getCurrentWidth(), NavigationBar.FRAME_HEIGHT);
			LOG.debug(
					"Update CubeScreen[{}] with state [" + screen.getState() + "] and position (" + screen.getPosX() + ":" + screen.getPosY() + ")("
							+ screen.getCurrentWidth() + "x" + screen.getCurrentHeight() + ")", id);
			// update active flag
			active = screen.getState() == State.CONNECTED_AND_ACTIVE;
			// update background size
			backgroundFrame.setVisible(active);
			backgroundFrame.setBounds(bgdBounds);
			// update navigation bar size
			navbar.setVisible(active);
			navbar.setBounds(navBounds);
		}

		private void moveAllTabsToAnotherScreen() {
			CubeScreen target = null;
			for (CubeScreen c : cubeScrs.values()) {
				if (c.isActive() && c != this) {
					target = c;
					break;
				}
			}
			if (target == null) {
				// if no other monitor is available. Put VM in 'Hidden'
				// menu.
				for (VmHandle h : client.listVms()) {
					if (h.getMonitorId().equals(id)) {
						core.setVmProperty(h.getVmId(), "hidden", "true", true);
					}
				}
				return;
			} else {
				// move all VMs in target screen.
				for (VmHandle h : client.listVms()) {
					if (h.getMonitorId().equals(id)) {
						vmControl.moveVm(h, target.getId());
					}
				}
			}
		}

		/** dispose all resources allocated so far. */
		public void dispose() {
			try {
				LOG.debug("Dispose CubeScreen [{}] and related resources", id);
				backgroundFrame.setVisible(false);
				navbar.setVisible(false);
				// move all tabs that are in this screen in another screen
				moveAllTabsToAnotherScreen();
				//
				backgroundFrame.dispose();
				navbar.dispose();
			} catch (Exception e) {
				LOG.error("Falied to dispose display.", e);
			}
		}

		public JFrame getBackgroundFrame() {
			return backgroundFrame;
		}

		public NavigationBar getNavigationBar() {
			return navbar;
		}

		public String getId() {
			return id;
		}

		public boolean isActive() {
			return active;
		}
	}
}
