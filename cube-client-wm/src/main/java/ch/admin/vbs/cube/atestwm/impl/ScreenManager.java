package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.org.apache.bcel.internal.generic.FNEG;

import ch.admin.vbs.cube.atestwm.IMessageManager;
import ch.admin.vbs.cube.atestwm.IMonitorLayout;
import ch.admin.vbs.cube.atestwm.IScreenManager;
import ch.admin.vbs.cube.atestwm.IMonitorLayout.IMonitorLayoutListener;
import ch.admin.vbs.cube.atestwm.ITabManager;
import ch.admin.vbs.cube.atestwm.IWindowManager;
import ch.admin.vbs.cube.atestwm.impl.ScreenManager.Screen;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;
import ch.admin.vbs.cube3.core.ui.MessageFrame;

public class ScreenManager implements IScreenManager, IMonitorLayoutListener {
	public static final String TABSFRAME_PREFIX = "ctsf#[";
	public static final String TABSFRAME_POSTFIX = "]";
	public static final String MSGSFRAME_PREFIX = "cmsf#[";
	public static final String MSGSFRAME_POSTFIX = "]";
	private static final Logger LOG = LoggerFactory.getLogger(ScreenManager.class);
	private HashMap<String, Screen> screens = new HashMap<String, Screen>();
	private IXrandr xrandr;
	private IWindowManager wm;
	private ITabManager tabManager;
	private IMessageManager msgManager;

	public void setup(IMonitorLayout layout, IXrandr xrandr, IWindowManager wm, ITabManager tabManager, IMessageManager msgManager) {
		this.xrandr = xrandr;
		this.wm = wm;
		this.tabManager = tabManager;
		this.msgManager = msgManager;
		layout.addListener(this);
	}

	@Override
	public void layoutChanged() {
		synchronizeScreenList();
	}

	@Override
	public MWindow getTabOrMsgWindow(String winName) {
		for (Screen screen : screens.values()) {
			if (screen.tabsPanel != null && screen.tabsPanel.getTitle().equals(winName)) {
				return screen.tabWindow;
			}
			if (screen.msgsPanel != null && screen.msgsPanel.getTitle().equals(winName)) {
				return screen.msgWindow;
			}
		}
		return null;
	}
	
	@Override
	public Screen getDefaultScreen() {
		if (screens.size() == 0) {
			LOG.error("No screen to return as default");
			return null;
		}
		return screens.entrySet().iterator().next().getValue();
	}
	
	@Override
	public MWindow getAppWindow(Window window) {
		for (Screen screen : screens.values()) {
			for (MWindow mw : screen.appWindows) {
				if (mw.getXClient()!= null && mw.getXClient().equals(window)) {
					return mw;
				}
			}
		}
		return null;
	}

	private void synchronizeScreenList() {
		HashSet<Screen> nScreen = new HashSet<Screen>();
		for (XRScreen s : xrandr.getScreens()) {
			if (s.getState() == State.CONNECTED_AND_ACTIVE || s.getState() == State.CONNECTED) {
				Screen screen = screens.get(s.getId());
				if (screen == null) {
					LOG.debug("Register a new Screen [{}]", s.getId());
					// new screen
					screen = new Screen(s);
					screens.put(screen.id, screen);
				} else {
					LOG.debug("Update existing Screen [{}]", s.getId());
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
		Rectangle scrBnds;
		TabFrame tabsPanel;
		MessageFrame msgsPanel;
		String id;
		MWindow bgWindow;
		MWindow tabWindow;
		MWindow msgWindow;
		ArrayList<MWindow> appWindows = new ArrayList<MWindow>();
		
		public Screen(XRScreen s) {
			id = s.getId();
			scrBnds = new Rectangle(s.getPosX(), s.getPosY(), s.getCurrentWidth(), s.getCurrentHeight());
			// create BG window
			Rectangle bgBnds = new Rectangle(//
					scrBnds.x, //
					scrBnds.y + TabManager.TAB_BAR_HEIGHT, //
					scrBnds.width, //
					scrBnds.height - TabManager.TAB_BAR_HEIGHT);
			bgWindow = wm.createAndMapWindow(bgBnds, 5);
			// create tab window (X)
			Rectangle tabBnds = new Rectangle(//
					scrBnds.x, //
					0, //
					scrBnds.width, //
					TabManager.TAB_BAR_HEIGHT);
			tabWindow = wm.createAndMapWindow(tabBnds, 5);
			// create msg window (X)
			Rectangle msgBnds = new Rectangle(//
					scrBnds.x, //
					0, //
					scrBnds.width/2, //
					scrBnds.height);
			msgWindow = wm.createAndMapWindow(msgBnds, 5);
			// create tab panel (JFrame)
			tabsPanel = tabManager.createTabPanel(fmtTabId(id), tabBnds);
			msgsPanel = msgManager.createTabPanel(fmtMsgId(id), tabBnds);
		}

		private void dispose() {
			LOG.debug("TODO: dispose XRScreen");
			// move managed window on another screen
			LOG.error("TODO: move managed window on another screen");
			// dispose BG
			wm.disposeWindow(bgWindow);
			// dispose tab frame
			tabManager.disposeTabPanel(fmtTabId(id));
			wm.disposeWindow(tabWindow);
			msgManager.disposeTabPanel(fmtMsgId(id));
			wm.disposeWindow(msgWindow);
		}

		private void update(XRScreen s) {
			Rectangle newBounds = new Rectangle(s.getPosX(), s.getPosY(), s.getCurrentWidth(), s.getCurrentHeight());
			// update tab window (X)
			Rectangle tabBnds = new Rectangle(//
					newBounds.x, //
					0, //
					newBounds.width, //
					TabManager.TAB_BAR_HEIGHT);
			wm.moveAndResizeWindow(tabWindow, tabBnds);
			// create tab panel (JFrame)
			tabManager.updateTabPanel(fmtTabId(id), tabBnds);
			// update BG window
			Rectangle bgBnds = new Rectangle(//
					newBounds.x, //
					newBounds.y + TabManager.TAB_BAR_HEIGHT, //
					newBounds.width, //
					newBounds.height - TabManager.TAB_BAR_HEIGHT);
			wm.moveAndResizeWindow(bgWindow, bgBnds);
			// update MSG window
			Rectangle msgBnds = new Rectangle(//
					newBounds.x, //
					newBounds.y, //
					newBounds.width/2, //
					newBounds.height);
			wm.moveAndResizeWindow(msgWindow, msgBnds);
			// update managed windows size and position
			LOG.error("TODO : update managed windows size and position");
		}
		
		
	}

	public class BgPanel {
		Rectangle bounds;
	}

	public static String fmtTabId(String id) {
		return new StringBuffer(TABSFRAME_PREFIX).append(id).append(TABSFRAME_POSTFIX).toString();
	}
	public static String fmtMsgId(String id) {
		return new StringBuffer(MSGSFRAME_PREFIX).append(id).append(MSGSFRAME_POSTFIX).toString();
	}
}
