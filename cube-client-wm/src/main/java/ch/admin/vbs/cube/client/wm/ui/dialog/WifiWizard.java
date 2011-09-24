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

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.common.CubeTransferType;
import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmHumanComparator;
import ch.admin.vbs.cube.core.vm.VmStatus;

import com.jidesoft.dialog.ButtonPanel;

/**
 * This dialog allows to choose a destination for the transferred files.
 */
public class WifiWizard extends CubeWizard {
	private static final long serialVersionUID = 0L;
	final static String CONNECTIONSLISTPANEL = "Connections List";
	final static String CONNECTIONDETAILSPANEL = "Connection Details";
	final static String CONNECTIONSECURITYPANEL = "Connection Security";
	private static ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
	// fields (connections overview)
	private JPanel connectionsListPanel;
	private JList connectionList;
	private JButton addBt, editBt, deleteBt, closeBt;
	// fields (connection details)
	private JPanel connectionDetailsPanel;
	private JComboBox SsidBox; // editables
	private JComboBox modeBox;
	private JButton cancelBt, saveBt, scanBt;
	// fields (security details)
	private JPanel connectionSecurityPanel;
	private JComboBox security; // None, wep, wpa
	private JPasswordField passphraseFld;
	//
	private DefaultComboBoxModel ssidMdl, modeMdl, securityMdl;
	private JPanel mainPanel;
	private CardLayout mainLayout;
	
	/**
	 * Creates a {@link WifiWizard}.
	 * 
	 */
	public WifiWizard() {
		super(null);
		// init models
		ssidMdl = new DefaultComboBoxModel();
		modeMdl = new DefaultComboBoxModel();
		securityMdl = new DefaultComboBoxModel();
		//
		setPreferredSize(new Dimension(600, 350));
	}

	@Override
	protected JPanel createCenterContentPanel() {
		// create sub-panels
		createConnectionListPanel();
		createConnectionDetailsPanel();
		createConnectionSecurityPanel();
		// create main panel
		mainPanel = new JPanel();
		mainLayout = new CardLayout();
		mainPanel.setLayout(mainLayout);
		mainPanel.add(connectionsListPanel, CONNECTIONSLISTPANEL);
		mainPanel.add(connectionDetailsPanel, CONNECTIONDETAILSPANEL);
		mainPanel.add(connectionSecurityPanel, CONNECTIONSECURITYPANEL);
		mainLayout.show(mainPanel, CONNECTIONSLISTPANEL);
		return mainPanel;
	}

	private void createConnectionSecurityPanel() {
		connectionSecurityPanel = new JPanel();
	}

	private void createConnectionDetailsPanel() {

		connectionDetailsPanel = new JPanel();
	}

	private void createConnectionListPanel() {
		connectionsListPanel = new JPanel();
	}

	@Override
	public ButtonPanel createButtonPanel() {
		return null;
//		// buttons
//		addWizardAction(new AbstractAction(resourceBundle.getString("filetransferWizard.button.cancel")) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				listener.cancelTransfer(filename, srcVm);
//			}
//		});
//		addWizardAction(new AbstractAction(resourceBundle.getString("filetransferWizard.button.ok")) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				listener.fileTransfer(filename, srcVm, (Vm) destFld.getSelectedItem());
//			}
//		}, true);
//		return super.createButtonPanel();
	}
}
