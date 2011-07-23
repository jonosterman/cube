package ch.admin.vbs.cube.client.wm.ui;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.ICubeClient;
import ch.admin.vbs.cube.client.wm.client.IVmControl;
import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.ui.tabs.NavigationBar;
import ch.admin.vbs.cube.client.wm.ui.wm.BackgroundFrame;
import ch.admin.vbs.cube.client.wm.xrandx.IXRListener;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.impl.XrandrTwoDisplayLayout;
import ch.admin.vbs.cube.client.wm.xrandx.impl.XrandrTwoDisplayLayout.Layout;
import ch.admin.vbs.cube.core.ICoreFacade;

public class CubeUI implements ICubeUI, IXRListener {
	private static final Logger LOG = LoggerFactory.getLogger(CubeUI.class);
	private IXrandr xrandr;
	private Object lock = new Object();
	private HashMap<String, CubeScreen> cubeScrs = new HashMap<String, CubeUI.CubeScreen>();
	private ICoreFacade core;
	private ICubeClient client;
	private IVmMonitor vmMonitor;
	private IVmControl vmControl;
	private ICubeUI cubeUI;
	private XrandrTwoDisplayLayout layoutMgr;
	private Layout currentLayout = Layout.AB;

	public void setup(ICoreFacade core, ICubeClient client, IXrandr xrandr, IVmMonitor vmMonitor, IVmControl vmControl, ICubeUI cubeUI) {
		this.core = core;
		this.client = client;
		this.vmMonitor = vmMonitor;
		this.vmControl = vmControl;
		this.cubeUI = cubeUI;
		this.xrandr = xrandr;
		this.xrandr.addListener(this);
		layoutMgr = new XrandrTwoDisplayLayout();
		// force first sync
		screenChanged();
	}

	@Override
	public void layoutScreens(Layout layout) {
		currentLayout = layout;
		layoutMgr.layout(layout, xrandr);
	}

	@Override
	public void screenChanged() {
		synchronized (lock) {
			// fetch display list & dimension
			List<XRScreen> screens = xrandr.getScreens();
			//
			updateCubeScreen(screens);
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
				if (c.isConnected()) {
					return c;
				}
			}
			LOG.error("No screen to return as default");
			return null;
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
			HashMap<String, CubeScreen> copy = (HashMap<String, CubeScreen>) cubeScrs.clone();
			for (XRScreen s : screens) {
				CubeScreen c = copy.remove(s.getId());
				if (c == null) {
					c = new CubeScreen();
					cubeScrs.put(s.getId(), c);
					c.init(s);
				} else {
					c.update(s);
				}
			}
			// removed screens
			for (CubeScreen c : copy.values()) {
				c.dispose();
				cubeScrs.remove(c.id);
			}
		}
	}

	/**
	 * CubeScreen hold a reference to the background frame, and the
	 * NavigationBar's frame
	 */
	public class CubeScreen implements ComponentListener {
		private JFrame backgroundFrame;
		private String id;
		private NavigationBar navbar;
		private boolean connected;

		public CubeScreen() {
		}

		public void init(XRScreen screen) {
			id = screen.getId();
			connected = screen.isConnected();
			// create navigation bar frame
			navbar = new NavigationBar(screen.getId());
			navbar.setup(vmMonitor, vmControl, core, client, cubeUI);
			navbar.setVisible(screen.isConnected());
			// create background frame
			backgroundFrame = new BackgroundFrame(this.id, new Rectangle(screen.getPosx(), screen.getPosy(), screen.getCurrentWidth(),
					screen.getCurrentHeight()));
			backgroundFrame.setVisible(screen.isConnected());
			backgroundFrame.addComponentListener(this);
		}

		/** Take care of size and state changes. */
		public void update(XRScreen screen) {
			connected = screen.isConnected();
			System.out.println("Update:::"+screen.isConnected());
			backgroundFrame.setVisible(screen.isConnected());
			navbar.setVisible(screen.isConnected());
			// update size / position
			backgroundFrame.setBounds(
			new Rectangle(screen.getPosx(), screen.getPosy(), screen.getCurrentWidth(),
					screen.getCurrentHeight()));
			navbar.setBounds(
					new Rectangle(screen.getPosx(), screen.getPosy(), screen.getCurrentWidth(),
							NavigationBar.FRAME_HEIGHT));
		}

		/** dispose all resources allocated so far. */
		public void dispose() {
			try {
				backgroundFrame.setVisible(false);
				navbar.setVisible(false);
				// move all tabs that are in this screen in another screen
				CubeScreen target = null;
				for (CubeScreen c : cubeScrs.values()) {
					if (c.isConnected() && c != this) {
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
				//
				backgroundFrame.dispose();
				navbar.dispose();
			} catch (Exception e) {
				LOG.error("Falied to dispose display.", e);
			}
		}

		private void updateNavigationBarDimension(Component component) {
			LOG.debug("Update bar position and dimension (" + component.getX() + ":" + component.getY() + ")(" + component.getWidth() + "x"
					+ NavigationBar.FRAME_HEIGHT + ")");
			navbar.setLocation(component.getX(), component.getY());
			navbar.setSize(component.getWidth(), NavigationBar.FRAME_HEIGHT);
		}

		// ################################################
		// ## ComponentListener
		// ################################################
		@Override
		public void componentHidden(ComponentEvent e) {
			updateNavigationBarDimension(e.getComponent());
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			updateNavigationBarDimension(e.getComponent());
		}

		@Override
		public void componentResized(ComponentEvent e) {
			updateNavigationBarDimension(e.getComponent());
		}

		@Override
		public void componentShown(ComponentEvent e) {
			updateNavigationBarDimension(e.getComponent());
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

		public boolean isConnected() {
			return connected;
		}
	}
}
