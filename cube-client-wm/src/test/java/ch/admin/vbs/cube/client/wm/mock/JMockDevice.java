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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.impl.TokenEvent;
import ch.admin.vbs.cube.core.impl.TokenEvent.EventType;

public class JMockDevice extends JPanel implements ITokenDevice, ActionListener {
	private static final long serialVersionUID = 1L;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(JMockDevice.class);
	private static final String LABEL_INSERT = "Insert Smart-Card";
	private static final String LABEL_REMOVE = "Remove Smart-Card";
	private JFrame frame;
	private JButton button;
	private boolean state = false;
	private ArrayList<ITokenListener> listeners = new ArrayList<ITokenListener>();
	
	public JMockDevice() {
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		state = !state;
		if (state) {
			button.setText(LABEL_REMOVE);
		} else {
			button.setText(LABEL_INSERT);
		}
		fireStateChanged(state);
	}
	@Override
	public void addListener(ITokenListener l) {
		listeners.add(l);
	}
	private void fireStateChanged(boolean newState) {
		LOG.debug("Token state changed [{}]. Fire event.", newState);
		TokenEvent event = new TokenEvent(newState ? EventType.TOKEN_INSERTED : EventType.TOKEN_REMOVED);
		for (ITokenListener l : listeners) {
			l.notifyTokenEvent(event);
		}
	}
	@Override
	public boolean isTokenReady() {
		return state;
	}

	@Override
	public void start() {
		frame = new JFrame();
		frame.setContentPane(this);
		setLayout(new GridLayout(1, 1));
		setPreferredSize(new Dimension(300, 100));
		button = new JButton(LABEL_INSERT);
		add(button);
		frame.pack();
		frame.setLocation(0,0);
		frame.setVisible(true);
		button.addActionListener(this);
	}
}
