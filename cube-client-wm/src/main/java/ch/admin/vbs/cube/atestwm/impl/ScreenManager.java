package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.org.apache.bcel.internal.generic.FNEG;

import ch.admin.vbs.cube.atestwm.IMonitorLayout;
import ch.admin.vbs.cube.atestwm.IScreenManager;
import ch.admin.vbs.cube.atestwm.IMonitorLayout.IMonitorLayoutListener;
import ch.admin.vbs.cube.atestwm.ITabManager;
import ch.admin.vbs.cube.atestwm.IWindowManager;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;

public class ScreenManager implements IScreenManager, IMonitorLayoutListener {
	public static final String TABSFRAME_PREFIX = "cube##tabsframe-[";
	public static final String TABSFRAME_POSTFIX = "]";
	private static final Logger LOG = LoggerFactory.getLogger(ScreenManager.class);
	private HashMap<String, Screen> screens = new HashMap<String, Screen>();
	private IXrandr xrandr;
	private IWindowManager wm;
	private ITabManager tabManager;

	public void setup(IMonitorLayout layout, IXrandr xrandr, IWindowManager wm, ITabManager tabManager) {
		this.xrandr = xrandr;
		this.wm = wm;
		this.tabManager = tabManager;
		layout.addListener(this);
	}

	@Override
	public void layoutChanged() {
		synchronizeScreenList();
	}

	@Override
	public Window getTabWindow(String winName) {
		for (Screen screen : screens.values()) {
			if (screen.tabsPanel != null && screen.tabsPanel.getTitle().equals(winName)) {
				return screen.tabWindow;
			}
		}
		return null;
	}

	private void synchronizeScreenList() {
		HashSet<Screen> nScreen = new HashSet<Screen>();
		for (XRScreen s : xrandr.getScreens()) {
			if (s.getState() == State.CONNECTED_AND_ACTIVE) {
				Screen screen = screens.get(s.getId());
				if (screen == null) {
					LOG.debug("Register a new Screen [{}]" + s.getPosX(), s.getId());
					// new screen
					screen = new Screen(s);
					screens.put(screen.id, screen);
				} else {
					LOG.debug("Update existing Screen [{}]" + s.getPosX(), s.getId());
					// update screen
					screen.update(s);
				}
				nScreen.add(screen);
			}
		}
		// remove unused screens
		ArrayList<Screen> old = new ArrayList<Screen>(screens.values());
		old.removeAll(nScreen);
		for (Screen sc : old) {
			LOG.debug("Dispose Screen [{}]", sc.id);
			sc.dispose();
			screens.remove(sc.id);
		}
	}

	public class Screen {
		Rectangle bounds;
		TabFrame tabsPanel;
		String id;
		Rectangle bgBnds;
		Window bgWindow;
		Rectangle tabBnds;
		Window tabWindow;

		public Screen(XRScreen s) {
			id = s.getId();
			bounds = new Rectangle(s.getPosX(), s.getPosY(), s.getCurrentWidth(), s.getCurrentHeight());
			// create BG window
			bgBnds = new Rectangle(//
					bounds.x, //
					bounds.y + TabManager.TAB_BAR_HEIGHT, //
					bounds.width, //
					bounds.height - TabManager.TAB_BAR_HEIGHT);
			bgWindow = wm.createAndMapWindow(bgBnds);
			// create tab window (X)
			tabBnds = new Rectangle(//
					bounds.x, //
					0, //
					bounds.width, //
					TabManager.TAB_BAR_HEIGHT);
			tabWindow = wm.createAndMapWindow(tabBnds);
			// create tab panel (JFrame)
			Rectangle tabPanelBnd = new Rectangle(0, 0, tabBnds.width, tabBnds.height);
			tabsPanel = tabManager.createTabPanel(fmtTabId(id), tabPanelBnd);
		}

		private void dispose() {
			// move managed window on another screen
			LOG.error("TODO");
			// dispose BG
			wm.disposeWindow(bgWindow);
			// dispose tab frame
			tabManager.disposeTabPanel(fmtTabId(id));
			wm.disposeWindow(tabWindow);
		}

		private void update(XRScreen s) {
			bounds = new Rectangle(s.getPosX(), s.getPosY(), s.getCurrentWidth(), s.getCurrentHeight());
			// update tab window (X)
			tabBnds = new Rectangle(//
					bounds.x, //
					0, //
					bounds.width, //
					TabManager.TAB_BAR_HEIGHT);
			wm.moveAndResizeWindow(tabWindow, tabBnds);
			// create tab panel (JFrame)
			Rectangle tabPanelBnd = new Rectangle(0, 0, tabBnds.width, tabBnds.height);
			tabManager.updateTabPanel(fmtTabId(id), tabPanelBnd);
			// update BG window
			bgBnds = new Rectangle(//
					bounds.x, //
					bounds.y + TabManager.TAB_BAR_HEIGHT, //
					bounds.width, //
					bounds.height - TabManager.TAB_BAR_HEIGHT);
			wm.moveAndResizeWindow(bgWindow, bgBnds);
			// update managed windows size and position
			LOG.error("TODO");
		}
	}

	

	public class BgPanel {
		Rectangle bounds;
	}

	public static String fmtTabId(String id) {
		return new StringBuffer(TABSFRAME_PREFIX).append(id).append(TABSFRAME_POSTFIX).toString();
	}
}
