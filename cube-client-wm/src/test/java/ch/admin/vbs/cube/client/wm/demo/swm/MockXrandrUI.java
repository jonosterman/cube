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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockXrandrUI extends JPanel {
	private static final Logger LOG = LoggerFactory.getLogger(MockXrandrUI.class);
	private JButton addMonitorButton;
	private JButton remMonitorButton;
	private OutputStream outClient;

	public static void main(String[] args) {
		MockXrandrUI ui = new MockXrandrUI();
		ui.start();
	}

	private void reconnect() {
		try {
			Socket client = new Socket("localhost", 9122);
			outClient = client.getOutputStream();
			LOG.debug("Reconnected");
		} catch (Exception e) {
			LOG.debug("Failed to reconnect");
		}
	}

	public MockXrandrUI() {
		// initial config
		buildUI();
	}

	private void buildUI() {
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		setPreferredSize(new Dimension(170, 70));
		addMonitorButton = new JButton("Add");
		add(addMonitorButton);
		remMonitorButton = new JButton("Rem");
		add(remMonitorButton);
		// layout
		layout.putConstraint(SpringLayout.NORTH, addMonitorButton, 10, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, addMonitorButton, 10, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, addMonitorButton, 70, SpringLayout.WEST, addMonitorButton);
		layout.putConstraint(SpringLayout.NORTH, remMonitorButton, 10, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, remMonitorButton, 10, SpringLayout.EAST, addMonitorButton);
		layout.putConstraint(SpringLayout.EAST, remMonitorButton, 70, SpringLayout.WEST, remMonitorButton);
		//
		addMonitorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					outClient.write(1);
					outClient.flush();
				} catch (Exception e) {
					reconnect();
				}
			}
		});
		remMonitorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ea) {
				try {
					outClient.write(2);
					outClient.flush();
				} catch (Exception e) {
					reconnect();
				}
			}
		});
	}

	public void start() {
		JFrame jframe = new JFrame("Monitors control");
		jframe.setContentPane(this);
		jframe.pack();
		jframe.setVisible(true);
		reconnect();
	}
}
