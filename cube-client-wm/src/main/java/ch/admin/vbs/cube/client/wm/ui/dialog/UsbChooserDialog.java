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
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.core.usb.UsbDevice;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntry;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntry.DeviceEntryState;

import com.jidesoft.dialog.ButtonPanel;

/**
 * This dialog displays a list of USB devices that user could bind to the
 * selected VM. The selected VM MUST be running.
 */
public class UsbChooserDialog extends CubeWizard {
	private static final long serialVersionUID = 0L;
	private static final Logger LOG = LoggerFactory.getLogger(UsbChooserDialog.class);
	/**
	 * Used for error messages.
	 */
	public static final int TYPE_CANCEL_YES = 1;
	private static final Dimension MINIMUM_DIALOG_SIZE = new Dimension(400, 247);
	private final String messageKey;
	private DefaultComboBoxModel deviceModel;
	private final IVmMonitor monitor;
	private JComboBox deviceFld;

	/**
	 * @param list 
	 * 
	 */
	public UsbChooserDialog(JFrame owner, String messageKey, IVmMonitor monitor, List<UsbDeviceEntry> list) {
		super(owner, "");
		this.messageKey = messageKey;
		this.monitor = monitor;
		//
		deviceModel = new DefaultComboBoxModel();
		try {
			for (UsbDeviceEntry d : list) {
				if (d.getState() == DeviceEntryState.AVAILABLE) {
					deviceModel.addElement(d.getDevice());
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to list usb devices", e);
		}
		setPreferredSize(new Dimension(600, 350));
	}

	@Override
	protected JPanel createCenterContentPanel() {
		ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
		// check for default message
		String messageText = null;
		try {
			messageText = resourceBundle.getString(messageKey);
		} catch (Exception e) {
			messageText = messageKey;
		}
		JPanel panel = new JPanel();
		// layout
		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);
		JTextArea message = new JTextArea();
		message.setFocusable(false);
		message.setEditable(false);
		message.setBackground(panel.getBackground());
		message.setText(messageText);
		message.setLineWrap(true);
		message.setWrapStyleWord(true);
		panel.add(message);
		// vm list
		JLabel device = new JLabel(resourceBundle.getString("usbdialog.label.device"));
		// target.setFont(target.getFont().deriveFont(Font.PLAIN));
		panel.add(device);
		deviceFld = new JComboBox(deviceModel);
		panel.add(deviceFld);
		// constraints
		layout.putConstraint(SpringLayout.NORTH, message, 30, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.WEST, message, 30, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, message, -30, SpringLayout.EAST, panel);
		// labels
		layout.putConstraint(SpringLayout.NORTH, device, 120, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.WEST, device, 30, SpringLayout.WEST, panel);
		// fields
		layout.putConstraint(SpringLayout.NORTH, deviceFld, -1, SpringLayout.NORTH, device);
		layout.putConstraint(SpringLayout.WEST, deviceFld, 200, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, deviceFld, -30, SpringLayout.EAST, panel);
		//
		deviceFld.setRenderer(new UsbDeviceListRenderer(monitor));
		setMinimumSize(MINIMUM_DIALOG_SIZE);
		return panel;
	}

	@Override
	public ButtonPanel createButtonPanel() {
		ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
		JButton cancelButton = addWizardAction(new AbstractAction(resourceBundle.getString("messagedialog.option.cancel")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setDialogResult(0);
				setVisible(false);
			}
		}, true);
		addWizardAction(new AbstractAction(resourceBundle.getString("messagedialog.option.connect")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setDialogResult(1);
				setVisible(false);
			}
		}, true);
		setDefaultAction(cancelButton.getAction());
		setInitFocusedComponent(cancelButton);
		return super.createButtonPanel();
	}

	public UsbDevice getSelection() {
		return (UsbDevice) deviceFld.getSelectedItem();
	}
}
