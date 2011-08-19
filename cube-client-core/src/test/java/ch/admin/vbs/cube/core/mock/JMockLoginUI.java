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

package ch.admin.vbs.cube.core.mock;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

import ch.admin.vbs.cube.core.ILoginUI;
import ch.admin.vbs.cube.core.impl.CallbackPin;

/**
 * MockLoginUI is used to simulate login UI. mht/2011.06.01: it seems buggy
 * (thread). by inserting/removing SC in a fast rate, some dialog are not
 * correctly resized (0x0 window).
 */
public class JMockLoginUI implements ILoginUI {
	private JDialog currentDialog;

	@Override
	public void showPinDialog(final String message, final CallbackPin callback) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JDialog dial = new JDialog();
				SpringLayout layout = new SpringLayout();
				JPanel panel = new JPanel(layout);
				panel.setPreferredSize(new Dimension(500, 300));
				dial.setContentPane(panel);
				dial.setVisible(true);
				//
				final JPasswordField pfield = new JPasswordField(10);
				panel.add(pfield);
				pfield.setText("111222");
				JLabel label = new JLabel("Enter Pin  (message:'" + message + "')");
				panel.add(label);
				final JButton loginBt = new JButton("Login");
				panel.add(loginBt);
				//
				layout.putConstraint(SpringLayout.WEST, label, 5, SpringLayout.WEST, panel);
				layout.putConstraint(SpringLayout.NORTH, label, 5, SpringLayout.NORTH, panel);
				layout.putConstraint(SpringLayout.WEST, pfield, 5, SpringLayout.WEST, panel);
				layout.putConstraint(SpringLayout.NORTH, pfield, 5, SpringLayout.SOUTH, label);
				layout.putConstraint(SpringLayout.WEST, loginBt, 5, SpringLayout.WEST, panel);
				layout.putConstraint(SpringLayout.NORTH, loginBt, 5, SpringLayout.SOUTH, pfield);
				//
				loginBt.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						pfield.setEnabled(false);
						loginBt.setEnabled(false);
						char[] input = pfield.getPassword();
						if (input == null || input.length == 0) {
							callback.aborted();
						} else {
							callback.setPassword(input);
							callback.process();
						}
					}
				});
				pfield.addKeyListener(new KeyListener() {
					@Override
					public void keyTyped(KeyEvent e) {
						if (e.getKeyChar() == 10) {
							pfield.setEnabled(false);
							loginBt.setEnabled(false);
							char[] input = pfield.getPassword();
							if (input == null || input.length == 0) {
								callback.aborted();
							} else {
								callback.setPassword(input);
								callback.process();
							}
						}
					}

					@Override
					public void keyReleased(KeyEvent e) {
					}

					@Override
					public void keyPressed(KeyEvent e) {
					}
				});
				// replace current dialog
				closeDialog();
				currentDialog = dial;
				dial.pack();
				dial.setLocationRelativeTo(null);
				dial.setModal(false);
				dial.setVisible(true);
				//
			}
		});
	}

	@Override
	public void closeDialog() {
		if (currentDialog != null) {
			currentDialog.setVisible(false);
			currentDialog.dispose();
		}
	}

	@Override
	public void showDialog(final String message, final LoginDialogType type) {
		System.out.println(">> ShowDialog [" + message + "]");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JDialog dial = new JDialog();
				SpringLayout layout = new SpringLayout();
				JPanel panel = new JPanel(layout);
				panel.setPreferredSize(new Dimension(500, 300));
				dial.setContentPane(panel);
				dial.setVisible(true);
				//
				JLabel label = new JLabel("[" + type + "] " + message);
				panel.add(label);
				//
				layout.putConstraint(SpringLayout.WEST, label, 5, SpringLayout.WEST, panel);
				layout.putConstraint(SpringLayout.NORTH, label, 5, SpringLayout.NORTH, panel);
				// replace current dialog
				closeDialog();
				currentDialog = dial;
				dial.pack();
				dial.setLocationRelativeTo(null);
				dial.setModal(false);
				dial.setVisible(true);
				//
			}
		});
	}
}
