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
package ch.admin.vbs.cube.client.wm.demo.swm;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.XRResolution;

public class DemoMonitorControl extends JPanel implements IXrandr {
	private HashMap<String, Monitor> monitors = new HashMap<String, DemoMonitorControl.Monitor>();
	private JList monList;
	private DefaultListModel monListModel = new DefaultListModel();
	private JButton addMonitorButton;
	private JButton remMonitorButton;
	private ArrayList<XRScreen> list;

	public DemoMonitorControl() {
		// initial config
		Monitor m0 = new Monitor("monitor#1", new Rectangle(0, 0, 64, 48));
		monitors.put(m0.id, m0);
		buildUI();
	}

	private void buildUI() {
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		setPreferredSize(new Dimension(300, 300));
		monList = new JList(monListModel);
		JScrollPane scroll = new JScrollPane(monList);
		monListModel.addElement(new Monitor("mon#" + monListModel.size(), new Rectangle(0, 0, 80, 60)));
		add(scroll);
		addMonitorButton = new JButton("Add");
		add(addMonitorButton);
		remMonitorButton = new JButton("Rem");
		add(remMonitorButton);
		// layout
		layout.putConstraint(SpringLayout.NORTH, scroll, 10, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, scroll, 10, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, scroll, -10, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, scroll, 100, SpringLayout.NORTH, scroll);
		//
		layout.putConstraint(SpringLayout.NORTH, addMonitorButton, 10, SpringLayout.SOUTH, scroll);
		layout.putConstraint(SpringLayout.WEST, addMonitorButton, 10, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, addMonitorButton, 70, SpringLayout.WEST, addMonitorButton);
		layout.putConstraint(SpringLayout.NORTH, remMonitorButton, 10, SpringLayout.SOUTH, scroll);
		layout.putConstraint(SpringLayout.WEST, remMonitorButton, 10, SpringLayout.EAST, addMonitorButton);
		layout.putConstraint(SpringLayout.EAST, remMonitorButton, 70, SpringLayout.WEST, remMonitorButton);
		//
		addMonitorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int rmd = new Random().nextInt(3);
				//
				switch (rmd) {
				case 0:
					monListModel.addElement(new Monitor("mon#" + monListModel.size(), new Rectangle(0, 0, 64, 48)));
					break;
				case 1:
					monListModel.addElement(new Monitor("mon#" + monListModel.size(), new Rectangle(0, 0, 102, 76)));
					break;
				case 2:
					monListModel.addElement(new Monitor("mon#" + monListModel.size(), new Rectangle(0, 0, 80, 60)));
					break;
				}
			}
		});
		remMonitorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (monListModel.size()>1) {
					monListModel.removeElementAt(monListModel.size()-1);
				}
			}
		});
	}

	public void start() {
		JFrame jframe = new JFrame("Monitors control");
		jframe.setContentPane(this);
		jframe.pack();
		jframe.setVisible(true);
	}

	private class Monitor {
		Rectangle bounds;
		String id;
		public boolean active = true;

		public Monitor(String id, Rectangle bounds) {
			this.id = id;
			this.bounds = bounds;
		}

		@Override
		public String toString() {
			return String.format("%s (%d:%d)(%dx%d)", id, bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}
	
	@Override
	public List<XRScreen> getScreens() {
		return list;
	}
	
	@Override
	public void reloadConfiguration() {
		list = new ArrayList<XRScreen>();
		for (int i = 0;i<monListModel.getSize();i++) {
			Monitor m = (Monitor) monListModel.get(i);
			List<XRResolution> resolutions = new ArrayList<XRScreen.XRResolution>();			
			XRResolution selectedResolution = new XRResolution(m.bounds.width, m.bounds.height, new ArrayList<String>());
			resolutions.add(selectedResolution);
			XRScreen s = new XRScreen(m.id, State.CONNECTED, m.bounds.x, m.bounds.y, resolutions, selectedResolution, "50");
			list.add(s);
		}		
	}
	
	@Override
	public void setScreen(XRScreen xrScreen, boolean active, int x, int y) {			
		for (int i = 0;i<monListModel.getSize();i++) {
			Monitor m = (Monitor) monListModel.get(i);
			if (m.id.equals(xrScreen.getId())) {
				m.bounds.x = x;
				m.bounds.y = y;
				m.active  = active;
			}
		}
		monListModel.setElementAt( monListModel.getElementAt(0),0);
	}
	
}
