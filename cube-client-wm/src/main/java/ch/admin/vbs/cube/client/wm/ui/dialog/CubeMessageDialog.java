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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.client.wm.utils.IconManager;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.JideOptionPane;

/**
 * Cube Dialog Message
 */
public class CubeMessageDialog extends CubeWizard {
	/** Logger */
	private static final long serialVersionUID = -116188699016257307L;
	/**
	 * Used for information messages.
	 */
	public static final int TYPE_INFOMATION = JideOptionPane.INFORMATION_MESSAGE;
	/**
	 * Used for warning messages.
	 */
	public static final int TYPE_WARNING = JideOptionPane.WARNING_MESSAGE;
	/**
	 * Used for error messages.
	 */
	public static final int TYPE_ERROR = JideOptionPane.ERROR_MESSAGE;
	/**
	 * Used for error messages.
	 */
	public static final int TYPE_PLAIN = JideOptionPane.PLAIN_MESSAGE;
	private static final Dimension MINIMUM_DIALOG_SIZE = new Dimension(400, 247);
	private final int messageType;
	private final String message;
	private final String title;

	/**
	 * Creates a information message dialog with the given message.
	 * 
	 * @param owner
	 *            the owner of the dialog, which the dialog belong to
	 * @param message
	 *            the message of the dialog
	 */
	public CubeMessageDialog(JFrame owner, String message) {
		this(owner, message, null, TYPE_INFOMATION);
	}

	/**
	 * Creates a information message dialog with the given message and title.
	 * 
	 * @param owner
	 *            the owner of the dialog, which the dialog belong to
	 * @param message
	 *            the message of the dialog
	 * @param title
	 *            the title of the dialog
	 */
	public CubeMessageDialog(JFrame owner, String message, String title) {
		this(owner, message, title, TYPE_INFOMATION);
	}

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
	public CubeMessageDialog(JFrame owner, String message, String title, int messageType) {
		super(owner, "");
		this.message = message;
		this.title = title;
		this.messageType = messageType;
	}

	@Override
	protected JPanel createCenterContentPanel() {
		ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
		String messageTypeLiteral;
		ImageIcon icon = null;
		switch (messageType) {
		case TYPE_PLAIN:
			messageTypeLiteral = "plain";
			icon = IconManager.getInstance().getIcon("empty-icon48.png");
			break;
		case TYPE_WARNING:
			messageTypeLiteral = "warning";
			icon = IconManager.getInstance().getIcon("warning-icon48.png");
			break;
		case TYPE_ERROR:
			messageTypeLiteral = "error";
			icon = IconManager.getInstance().getIcon("error-icon48.png");
			break;
		default:
			messageTypeLiteral = "information";
			icon = IconManager.getInstance().getIcon("info-icon48.png");
			break;
		}
		// check for default title
		if (title == null) {
			setTitle(resourceBundle.getString("messagedialog.default." + messageTypeLiteral + ".title"));
		} else {
			setTitle(title);
		}
		// check for default message
		String messageText = message;
		if (message == null) {
			messageText = resourceBundle.getString("messagedialog.default." + messageTypeLiteral + ".message");
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
		addWizardAction(new AbstractAction("OK") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
			}
		}, true);
		return super.createButtonPanel();
	}
}
