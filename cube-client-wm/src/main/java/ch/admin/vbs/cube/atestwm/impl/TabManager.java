package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Spring;
import javax.swing.SpringLayout;

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
	private static final Logger LOG = LoggerFactory.getLogger(TabManager.class);
	public static final int TAB_BAR_HEIGHT = 25;
	private HashMap<String, TabFrame> tabPanels = new HashMap<String, TabFrame>();
	private IXrandr xrandr;

	public void setup(IMonitorLayout layout, IXrandr xrandr) {
		this.xrandr = xrandr;
	}

	@Override
	public TabFrame createTabPanel(String fId, Rectangle bounds) {
		TabFrame frame = new TabFrame(fId);
		JPanel p = new JPanel();
		p.setPreferredSize(new Dimension(bounds.width, TAB_BAR_HEIGHT));
		p.setBackground(Color.PINK);
		SpringLayout layout = new SpringLayout();
		p.setLayout(layout);
		JLabel l = new JLabel("L:"+fId);
		JLabel r = new JLabel(":R");
		p.add(l);
		p.add(r);
		layout.putConstraint(SpringLayout.NORTH,l,2,SpringLayout.NORTH,p);
		layout.putConstraint(SpringLayout.WEST,l,2,SpringLayout.WEST,p);
		layout.putConstraint(SpringLayout.NORTH,r,2,SpringLayout.NORTH,p);
		layout.putConstraint(SpringLayout.EAST,r,-2,SpringLayout.EAST,p);		
		//
		tabPanels.put(fId, frame);
		//
		frame.setContentPane(p);
		frame.pack();
		frame.setVisible(true);
		return frame;
	}
	@Override
	public void disposeTabPanel(String fId) {
		TabFrame tf = tabPanels.get(fId);
		tf.setVisible(false);
		tf.dispose();
	}
	@Override
	public void updateTabPanel(String fId, Rectangle bounds) {
		TabFrame tf = tabPanels.get(fId);
		tf.setLocation(bounds.x, bounds.y);
		tf.setPreferredSize(new Dimension(bounds.width, bounds.height));
		tf.pack();
	}
	
	@Override
	public boolean isTabPanel(String winName) {
		return tabPanels.containsKey(winName);
	}
	
//	public class MonitorLayoutHandler implements IMonitorLayoutListener {
//		@Override
//		public void layoutChanged() {
//			HashSet<TabFrame> aPanels = new HashSet<TabFrame>();
//			for (XRScreen s : xrandr.getScreens()) {
//				if (s.getState() == State.CONNECTED_AND_ACTIVE) {
//					// make sure that a bg frame is define for each screen
//					TabFrame frame = tabPanels.get(fmtTabId(s));
//					if (frame == null) {
//						LOG.debug("Create tab panel for screen {}", s.getId());
//						// create new tab panel
//						aPanels.add(frame);
//					} else {
//						LOG.debug("Update tab panel for screen {}", s.getId());
//						frame.getContentPane().setPreferredSize(new Dimension(s.getCurrentWidth(), TAB_BAR_HEIGHT));
//						frame.setBoundsAbsolute(new Rectangle(s.getPosX(), s.getPosY(), s.getCurrentWidth(), TAB_BAR_HEIGHT));
//						frame.pack();
//						aPanels.add(frame);
//					}
//				}
//			}
//			// remove unused bg windows
//			ArrayList<TabFrame> old = new ArrayList<TabFrame>(tabPanels.values());
//			old.removeAll(aPanels);
//			for (TabFrame tf : old) {
//				LOG.debug("Remove {}", tf.getTitle());
//				// TODO: re-parent window in deleted bg window if any.
//				tabPanels.remove(tf.getTitle());
//				tf.setVisible(false);
//				tf.dispose();
//			}
//		}
//	}

//	@Override
//	public Rectangle getTabBounds(String frameTitle) {
//		LOG.debug("getTabBounds [" + frameTitle + "]");
//		TabFrame frame = tabPanels.get(frameTitle);
//		return frame.getBoundsAbsolute();
//	}


}
