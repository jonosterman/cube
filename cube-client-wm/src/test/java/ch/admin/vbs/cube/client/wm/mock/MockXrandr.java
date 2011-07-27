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

package ch.admin.vbs.cube.client.wm.mock;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.XRResolution;

public class MockXrandr implements IXrandr {
	private ArrayList<XRScreen> screens = new ArrayList<XRScreen>();
	private ArrayList<XRResolution> res = new ArrayList<XRScreen.XRResolution>(2);
	private ArrayList<String> freqs = new ArrayList<String>(2);
	private Random rn = new Random();
	private Object lock = new Object();
	private DefaultListModel list = new DefaultListModel();

	public MockXrandr() {
		freqs.add("50.0");
		res.add(new XRResolution(320, 200, freqs));
		res.add(new XRResolution(640, 480, freqs));
		res.add(new XRResolution(800, 600, freqs));
		screens.add(new XRScreen("screen-A", State.CONNECTED_AND_ACTIVE, 60, 60, res, res.get(1), "50.0"));
		screens.add(new XRScreen("screen-B", State.CONNECTED_AND_ACTIVE, 720, 60, res, res.get(2), "50.0"));
		list.addElement(screens.get(0));
		list.addElement(screens.get(1));
	}

	
	@Override
	public void reloadConfiguration() {
		// TODO Auto-generated method stub
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<XRScreen> getScreens() {
		synchronized (lock) {
			return (List<XRScreen>) screens.clone();
		}
	}

	@Override
	public void setScreen(XRScreen xrScreen, boolean connected, int x, int y) {
		XRScreen s = new XRScreen(xrScreen.getId(), //
				connected ? State.CONNECTED_AND_ACTIVE : State.DISCONNECTED, //
				x, //
				y, //
				xrScreen.getResolutions(),//
				xrScreen.getSelectedResolution(),//
				xrScreen.getSelectedFrequency());
		for (int i = 0; i < screens.size(); i++) {
			if (screens.get(i).getId().equals(xrScreen.getId())) {
				screens.set(i, s);
				return;
			}
		}
	}

	@Override
	public void start() {
		JFrame screenCount = new JFrame("Heads");
		screenCount.setUndecorated(true);
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		screenCount.setContentPane(p);
		p.setPreferredSize(new Dimension(300, 200));
		final SpinnerModel model = new SpinnerNumberModel(2, 1, 5, 1);
		model.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				synchronized (lock) {
					int t = (Integer) model.getValue();
					while (screens.size() < t) {
						screens.add(new XRScreen("screen-" + screens.size(), //
								rn.nextBoolean() ? State.CONNECTED_AND_ACTIVE : State.DISCONNECTED, //
								0, 0, res, res.get(rn.nextInt(res.size())), "50.0"));
						list.add(screens.size() - 1, screens.get(screens.size() - 1));
					}
					while (screens.size() > t) {
						screens.remove(screens.size() - 1);
						list.removeElementAt(screens.size());
					}
				}
			}
		});
		JSpinner spin = new JSpinner(model);
		p.add(spin, BorderLayout.NORTH);
		JList l = new JList(list);
		p.add(l, BorderLayout.CENTER);
		screenCount.pack();
		screenCount.setLocation(0, Toolkit.getDefaultToolkit().getScreenSize().height - 200);
		screenCount.setVisible(true);
	}
}
