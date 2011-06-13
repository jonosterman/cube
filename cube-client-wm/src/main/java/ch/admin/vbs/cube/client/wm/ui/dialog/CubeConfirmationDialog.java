/**
 * Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.client.wm.utils.IconManager;

import com.jidesoft.dialog.ButtonPanel;

/**
 * 
 */
public class CubeConfirmationDialog extends CubeWizard {
	/** Logger */
	private static final long serialVersionUID = -116188699016257307L;
	/**
	 * Used for error messages.
	 */
	public static final int TYPE_CANCEL_YES = 1;
	private static final Dimension MINIMUM_DIALOG_SIZE = new Dimension(400, 247);
	private final int messageType;
	private final String messageKey;

	/**
	 * Creates a message dialog with the given message, title and message type.
	 * 
	 * @param owner
	 *            the owner of the dialog, which the dialog belong to
	 * @param message
	 *            the message of the dialog
	 * @param title
	 *            the title of the dialog
	 * @param messageType
	 *            the message type of the dialog, which takes the fitting icon
	 */
	public CubeConfirmationDialog(JFrame owner, String messageKey, int messageType) {
		super(owner, "");
		this.messageKey = messageKey;
		this.messageType = messageType;
	}

	@Override
	protected JPanel createCenterContentPanel() {
		ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
		ImageIcon icon = null;
		switch (messageType) {
		case TYPE_CANCEL_YES:
		default:
			icon = IconManager.getInstance().getIcon("warning-icon48.png");
			break;
		}
		// check for default message
		String messageText = null;
		try {
			messageText = resourceBundle.getString(messageKey);
		} catch (Exception e) {
			messageText = messageKey;
		}
		JPanel contentPnl = new JPanel();
		// layout
		SpringLayout layout = new SpringLayout();
		contentPnl.setLayout(layout);
		JTextArea messageArea = new JTextArea(messageText);
		messageArea.setEditable(false);
		messageArea.setFocusable(false);
		messageArea.setOpaque(false);
		messageArea.setLineWrap(true);
		messageArea.setWrapStyleWord(true);
		// messageArea.setBackground(getBackground());
		messageArea.setFont(new JLabel().getFont());
		contentPnl.add(messageArea);
		JLabel iconLb = new JLabel(icon);
		contentPnl.add(iconLb);
		// constraints
		layout.putConstraint(SpringLayout.NORTH, iconLb, 40, SpringLayout.NORTH, contentPnl);
		layout.putConstraint(SpringLayout.WEST, iconLb, 20, SpringLayout.WEST, contentPnl);
		layout.putConstraint(SpringLayout.NORTH, messageArea, 10, SpringLayout.NORTH, iconLb);
		layout.putConstraint(SpringLayout.SOUTH, messageArea, -20, SpringLayout.SOUTH, contentPnl);
		layout.putConstraint(SpringLayout.WEST, messageArea, 20, SpringLayout.EAST, iconLb);
		layout.putConstraint(SpringLayout.EAST, messageArea, -20, SpringLayout.EAST, contentPnl);
		setMinimumSize(MINIMUM_DIALOG_SIZE);
		return contentPnl;
	}

	@Override
	public ButtonPanel createButtonPanel() {
		ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
		switch (messageType) {
		case TYPE_CANCEL_YES:
		default:
			addWizardAction(new AbstractAction(resourceBundle.getString("messagedialog.option.yes")) {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent arg0) {
					setDialogResult(1);
					setVisible(false);
				}
			}, true);
			JButton cancelButton = addWizardAction(new AbstractAction(resourceBundle.getString("messagedialog.option.cancel")) {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent arg0) {
					setDialogResult(0);
					setVisible(false);
				}
			}, true);
			setDefaultAction(cancelButton.getAction());
			setInitFocusedComponent(cancelButton);
			break;
		}
		return super.createButtonPanel();
	}
}
