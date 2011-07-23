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

package ch.admin.vbs.cube.client.wm.ui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SpringLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.client.wm.utils.IconManager;
import ch.admin.vbs.cube.common.shell.ShellUtil;

import com.jidesoft.dialog.ButtonPanel;

/**
 * This class represent a password dialog. Title and message have a default
 * text. This means, that if no text was set, inclusive null, the default text
 * will be taken from the resource bundle. Use one of the display() to show the
 * dialog.
 * 
 */
public class PasswordDialog extends CubeWizard {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(PasswordDialog.class);
	private static final long serialVersionUID = -8554410616687766981L;
	private static final Dimension MINIMUM_DIALOG_SIZE = new Dimension(250, 50);
	private JLabel messageLbl;
	private JPasswordField passwordFld;
	private LinkedList<PasswordDialogListener> listeners;
	private JPanel contentPnl;
	private String additionalMessage;
	private ExecutorService execs = Executors.newCachedThreadPool();
	private JButton okButton;

	/**
	 * Creates password dialog with default title and message.
	 * 
	 * @param owner
	 *            the owner of the dialog, which the dialog belong to
	 */
	public PasswordDialog(JFrame owner) {
		super(owner, null);
		listeners = new LinkedList<PasswordDialogListener>();
		// make sure closing event is a cancel
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				notifyListenersAndClose(true);
			}
		});
		setModal(false);
	}

	@Override
	protected JPanel createCenterContentPanel() {
		// create content elements
		contentPnl = new JPanel();
		passwordFld = new JPasswordField(10);
		messageLbl = new JLabel();
		JLabel iconLbl = new JLabel(IconManager.getInstance().getIcon("keys-icon48.png"));
		JLabel labelLbl = new JLabel("PIN");
		// layout
		SpringLayout layout = new SpringLayout();
		contentPnl.setLayout(layout);
		contentPnl.add(messageLbl);
		contentPnl.add(iconLbl);
		contentPnl.add(passwordFld);
		contentPnl.add(labelLbl);
		//
		if (additionalMessage != null && additionalMessage.length() > 0) {
			messageLbl.setText(additionalMessage);
			messageLbl.setForeground(new Color(100, 100, 100));
		} else {
			messageLbl.setText("");
		}
		// constraints
		layout.putConstraint(SpringLayout.NORTH, iconLbl, 50, SpringLayout.NORTH, contentPnl);
		layout.putConstraint(SpringLayout.WEST, iconLbl, 20, SpringLayout.WEST, contentPnl);
		layout.putConstraint(SpringLayout.NORTH, messageLbl, 20, SpringLayout.NORTH, contentPnl);
		layout.putConstraint(SpringLayout.WEST, messageLbl, 20, SpringLayout.EAST, iconLbl);
		layout.putConstraint(SpringLayout.NORTH, labelLbl, 0, SpringLayout.NORTH, iconLbl);
		layout.putConstraint(SpringLayout.WEST, labelLbl, 20, SpringLayout.EAST, iconLbl);
		layout.putConstraint(SpringLayout.NORTH, passwordFld, 10, SpringLayout.SOUTH, labelLbl);
		layout.putConstraint(SpringLayout.WEST, passwordFld, 20, SpringLayout.EAST, iconLbl);
		setPreferredSize(MINIMUM_DIALOG_SIZE);
		return contentPnl;
	}

	/**
	 * Adds a PasswordDialogListener.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void addPasswordDialogListener(PasswordDialogListener listener) {
		synchronized (listener) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes a PasswordDialogListener.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void removePasswordDialogListener(PasswordDialogListener listener) {
		synchronized (listener) {
			listeners.remove(listener);
		}
	}

	/**
	 * Notifies the listener about the closing event.
	 * 
	 * @param canceled
	 *            true if the window was closed or cancel was pressed, otherwise
	 *            false
	 */
	private void notifyListenersAndClose(final boolean canceled) {
		passwordFld.setEnabled(false);
		okButton.setEnabled(false);
		execs.execute(new Runnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				List<PasswordDialogListener> cloned;
				synchronized (listeners) {
					cloned = (List<PasswordDialogListener>) listeners.clone();
				}
				for (PasswordDialogListener listener : cloned) {
					if (canceled) {
						listener.quit(null);
					} else {
						listener.quit(passwordFld.getPassword());
					}
				}
			}
		});
	}

	public void displayWizard(String additionalMessage) {
		this.additionalMessage = additionalMessage;
		try {
			ShellUtil su = new ShellUtil();
			su.run(Arrays.asList("numlockx", "on"));
		} catch (Exception e) {
			LOG.error("Failed to set NUM_LOCK on [" + e.getMessage() + "]");
		}
		super.displayWizard();
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jidesoft.dialog.StandardDialog#createButtonPanel()
	 */
	@Override
	public ButtonPanel createButtonPanel() {
		ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
		okButton = addWizardAction(new AbstractAction(resourceBundle.getString("passworddialog.button.login")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				notifyListenersAndClose(false);
			}
		}, true);
		return super.createButtonPanel();
	}
}
