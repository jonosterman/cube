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
import javax.swing.JMenu;
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
	public TabFrame createTabPanel(String fId, Rectangle bounds) {
		final TabFrame frame = new TabFrame(fId);
		final JPanel p = new JPanel();
		p.setPreferredSize(new Dimension(bounds.width, TAB_BAR_HEIGHT));
		p.setBackground(Color.PINK);
		SpringLayout layout = new SpringLayout();
		p.setLayout(layout);
		JLabel l = new JLabel("L:" + fId);
		JLabel r = new JLabel(":R");
		p.add(l);
		p.add(r);
		layout.putConstraint(SpringLayout.NORTH, l, 2, SpringLayout.NORTH, p);
		layout.putConstraint(SpringLayout.WEST, l, 2, SpringLayout.WEST, p);
		layout.putConstraint(SpringLayout.NORTH, r, 2, SpringLayout.NORTH, p);
		layout.putConstraint(SpringLayout.EAST, r, -2, SpringLayout.EAST, p);
		//
		final JPopupMenu pmenu = new JPopupMenu();
		for (int i = 1; i < 3; i++) {
			JMenuItem menu = new JMenuItem("start vm#" + i);
			final String vmid = "dev-vm"+i;
			pmenu.add(menu);
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					VirtualMachine vm = new VirtualMachine(vmid);					
					//vmMgr.command(vm, Command.START);
				}
			});
			menu = new JMenuItem("stop vm#" + i);
			pmenu.add(menu);
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					VirtualMachine vm = new VirtualMachine(vmid);					
					vmMgr.command(vm, Command.STOP);
				}
			});
		}
		p.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					pmenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		//
		tabPanels.put(fId, frame);
		//
		LOG.debug("Create Tab Frame [{}]", fId);
		frame.setContentPane(p);
		frame.pack();
		//frame.setLocation(bounds.x, bounds.y);
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
		LOG.debug("Move/resize JFrame {}", BoundFormatterUtil.format(bounds));
		tf.setPreferredSize(new Dimension(bounds.width, bounds.height));
		tf.pack();
	}

	@Override
	public boolean matchTabPanel(String winName) {
		return tabPanels.containsKey(winName);
	}
}
