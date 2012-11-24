package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SpringLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.ITabManager;
import ch.admin.vbs.cube3.core.IVMMgr;
import ch.admin.vbs.cube3.core.IVMMgr.Command;
import ch.admin.vbs.cube3.core.VirtualMachine;

public class TabManager implements ITabManager {
	private static final Logger LOG = LoggerFactory.getLogger(TabManager.class);
	public static final int TAB_BAR_HEIGHT = 25;
	private HashMap<String, TabFrame> tabPanels = new HashMap<String, TabFrame>();
	private IVMMgr vmMgr;

	public void setup(IVMMgr vmMgr) {
		this.vmMgr = vmMgr;
	}

	@Override
	public TabFrame createPanel(String fId, Rectangle bounds) {
		final TabFrame frame = new TabFrame(fId, vmMgr);
		tabPanels.put(fId, frame);
		//frame.setLocation(bounds.x, bounds.y);
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
		LOG.debug("Move/resize JFrame {}", BoundFormatterUtil.format(bounds));
		tf.setPreferredSize(new Dimension(bounds.width, bounds.height));
		tf.pack();
	}

	@Override
	public boolean matchTabPanel(String winName) {
		return tabPanels.containsKey(winName);
	}
}
