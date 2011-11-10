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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.common.CubeTransferType;
import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmHumanComparator;
import ch.admin.vbs.cube.core.vm.VmState;

import com.jidesoft.dialog.ButtonPanel;

/**
 * This dialog allows to choose a destination for the transferred files.
 */
public class FileTransferWizard extends CubeWizard {
	private static final long serialVersionUID = 0L;
	private Vm srcVm;
	private RelativeFile filename;
	private FileTransferWizardListener listener;
	private static ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
	private JLabel sourceFld;
	private JLabel fileFld;
	private JComboBox destFld;
	private JComboBox transFld;
	private DefaultComboBoxModel dstModel;
	private DefaultComboBoxModel trfModel;

	/**
	 * Creates a {@link FileTransferWizard}.
	 * 
	 * @param srcVm
	 *            the virtual machine from which a file is being exported.
	 * @param transferFileName
	 *            the name of the file or directory being transferred.
	 * @param vmMap
	 *            the map containing all virtual machines accessible to the
	 *            user.
	 * @param listener
	 *            the listener listening for the user decision.
	 */
	public FileTransferWizard(Vm srcVm, RelativeFile transferFileName, List<Vm> vms, FileTransferWizardListener listener) {
		super(null);
		this.srcVm = srcVm;
		this.filename = transferFileName;
		this.listener = listener;
		//
		dstModel = new DefaultComboBoxModel();
		refreshDestinationList(vms);
		setPreferredSize(new Dimension(600, 350));
	}

	private void refreshDestinationList(List<Vm> vms) {
		// sort
		TreeSet<Vm> sorted = new TreeSet<Vm>(new VmHumanComparator());
		sorted.addAll(vms);
		// update to model
		dstModel.removeAllElements();
		for (Vm v : sorted) {
			if (VmState.RUNNING.equals(v.getVmState())) {
				if (!srcVm.getId().equals(v.getId())) {
					dstModel.addElement(v);
				}
			}
		}
	}

	@Override
	protected JPanel createCenterContentPanel() {
		JPanel panel = new JPanel();
		// message
		JTextArea message = new JTextArea();
		message.setFocusable(false);
		message.setEditable(false);
		message.setBackground(panel.getBackground());
		message.setText(resourceBundle.getString("filetransferWizard.message.transferingfile"));
		message.setLineWrap(true);
		message.setWrapStyleWord(true);
		panel.add(message);
		// source VM
		JLabel source = new JLabel(resourceBundle.getString("filetransferWizard.label.source_vm"));
		source.setFont(source.getFont().deriveFont(Font.PLAIN));
		panel.add(source);
		sourceFld = new JLabel(srcVm.getDescriptor().getRemoteCfg().getName() + " (" + srcVm.getDescriptor().getRemoteCfg().getDomain() + ")");
		panel.add(sourceFld);
		// source file
		JLabel file = new JLabel(resourceBundle.getString("filetransferWizard.label.filename"));
		file.setFont(file.getFont().deriveFont(Font.PLAIN));
		panel.add(file);
		fileFld = new JLabel(filename.getRelativeFilename());
		panel.add(fileFld);
		// destination VM list
		JLabel dest = new JLabel(resourceBundle.getString("filetransferWizard.label.destination_vm"));
		dest.setFont(file.getFont().deriveFont(Font.PLAIN));
		panel.add(dest);
		destFld = new JComboBox(dstModel);
		panel.add(destFld);
		// transfer type
		JLabel trans = new JLabel(resourceBundle.getString("filetransferWizard.label.flavor"));
		trans.setFont(file.getFont().deriveFont(Font.PLAIN));
		panel.add(trans);
		trfModel = new DefaultComboBoxModel();
		for (CubeTransferType tt : CubeTransferType.values()) {
			trfModel.addElement(tt);
		}
		transFld = new JComboBox(trfModel);
		transFld.setRenderer(new CubeTransferTypeListRenderer());
		panel.add(transFld);
		// layout
		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);
		layout.putConstraint(SpringLayout.NORTH, message, 30, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.WEST, message, 30, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, message, -30, SpringLayout.EAST, panel);
		// labels
		layout.putConstraint(SpringLayout.NORTH, source, 120, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.WEST, source, 30, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.NORTH, file, 9, SpringLayout.SOUTH, source);
		layout.putConstraint(SpringLayout.WEST, file, 30, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.NORTH, dest, 9, SpringLayout.SOUTH, file);
		layout.putConstraint(SpringLayout.WEST, dest, 30, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.NORTH, trans, 9, SpringLayout.SOUTH, dest);
		layout.putConstraint(SpringLayout.WEST, trans, 30, SpringLayout.WEST, panel);
		// fields
		layout.putConstraint(SpringLayout.NORTH, sourceFld, 0, SpringLayout.NORTH, source);
		layout.putConstraint(SpringLayout.WEST, sourceFld, 200, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, sourceFld, -30, SpringLayout.EAST, panel);
		layout.putConstraint(SpringLayout.NORTH, fileFld, 0, SpringLayout.NORTH, file);
		layout.putConstraint(SpringLayout.WEST, fileFld, 200, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, fileFld, -30, SpringLayout.EAST, panel);
		layout.putConstraint(SpringLayout.NORTH, destFld, -2, SpringLayout.NORTH, dest);
		layout.putConstraint(SpringLayout.WEST, destFld, 200, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, destFld, -30, SpringLayout.EAST, panel);
		layout.putConstraint(SpringLayout.NORTH, transFld, -2, SpringLayout.NORTH, trans);
		layout.putConstraint(SpringLayout.WEST, transFld, 200, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, transFld, -30, SpringLayout.EAST, panel);
		// renderer
		destFld.setRenderer(new VmListRenderer());
		return panel;
	}

	@Override
	public ButtonPanel createButtonPanel() {
		// buttons
		addWizardAction(new AbstractAction(resourceBundle.getString("filetransferWizard.button.cancel")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				listener.cancelTransfer(filename, srcVm);
			}
		});
		addWizardAction(new AbstractAction(resourceBundle.getString("filetransferWizard.button.ok")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				listener.fileTransfer(filename, srcVm, (Vm) destFld.getSelectedItem());
			}
		}, true);
		return super.createButtonPanel();
	}
}
