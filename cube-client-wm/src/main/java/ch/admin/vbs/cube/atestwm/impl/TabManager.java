package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JLabel;

import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.IMonitorLayout;
import ch.admin.vbs.cube.atestwm.IMonitorLayout.IMonitorLayoutListener;
import ch.admin.vbs.cube.atestwm.ITabManager;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;

public class TabManager implements ITabManager {
	public static final String TABSFRAME_PREFIX = "cube##tabsframe-[";
	public static final String TABSFRAME_POSTFIX = "]";
	private static final Logger LOG = LoggerFactory.getLogger(TabManager.class);
	public static final int TAB_BAR_HEIGHT = 25;
	private HashMap<String, TabFrame> tabPanels = new HashMap<String, TabFrame>();
	private IXrandr xrandr;

	public void setup(IMonitorLayout layout, IXrandr xrandr) {
		this.xrandr = xrandr;
		layout.addListener(new MonitorLayoutHandler());
	}

	public class MonitorLayoutHandler implements IMonitorLayoutListener {
		@Override
		public void layoutChanged() {
			HashSet<TabFrame> aPanels = new HashSet<TabFrame>();
			for (XRScreen s : xrandr.getScreens()) {
				if (s.getState() == State.CONNECTED_AND_ACTIVE) {
					// make sure that a bg frame is define for each screen
					TabFrame frame = tabPanels.get(fmtTabId(s));
					if (frame == null) {
						LOG.debug("Create tab panel for screen {}", s.getId());
						// create new tab panel
						frame = new TabFrame(fmtTabId(s));
						frame.getContentPane().setPreferredSize(new Dimension(s.getCurrentWidth(), TAB_BAR_HEIGHT));
						frame.getContentPane().setBackground(Color.PINK);
						frame.getContentPane().add(new JLabel(fmtTabId(s)));
						frame.setBoundsAbsolute(new Rectangle(s.getPosX(), s.getPosY(), s.getCurrentWidth(), TAB_BAR_HEIGHT));
						tabPanels.put(fmtTabId(s), frame);
						frame.pack();
						frame.setVisible(true);
						aPanels.add(frame);
					} else {
						LOG.debug("Update tab panel for screen {}", s.getId());
						frame.getContentPane().setPreferredSize(new Dimension(s.getCurrentWidth(), TAB_BAR_HEIGHT));
						frame.setBoundsAbsolute(new Rectangle(s.getPosX(), s.getPosY(), s.getCurrentWidth(), TAB_BAR_HEIGHT));
						frame.pack();
						aPanels.add(frame);
					}
				}
			}
			// remove unused bg windows
			ArrayList<TabFrame> old = new ArrayList<TabFrame>(tabPanels.values());
			old.removeAll(aPanels);
			for (TabFrame tf : old) {
				LOG.debug("Remove {}", tf.getTitle());
				// TODO: re-parent window in deleted bg window if any.
				tabPanels.remove(tf.getTitle());
				tf.setVisible(false);
				tf.dispose();
			}
		}
	}

	@Override
	public Rectangle getTabBounds(String frameTitle) {
		LOG.debug("getTabBounds [" + frameTitle + "]");
		TabFrame frame = tabPanels.get(frameTitle);
		return frame.getBoundsAbsolute();
	}

	public String fmtTabId(XRScreen s) {
		return new StringBuffer(TABSFRAME_PREFIX).append(s.getId()).append(TABSFRAME_POSTFIX).toString();
	}
}
