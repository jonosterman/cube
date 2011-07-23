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

package ch.admin.vbs.cube.client.wm.ui.wm;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.utils.IconManager;
import ch.admin.vbs.cube.common.CubeClassification;

public class OsdFrame extends JFrame {
	private static final int LEFT_MARGIN = 50;
	private static final long serialVersionUID = 1L;
	// private static final Dimension OSD_DIMENSION = new Dimension(640,480);
	private static final Dimension OSD_DIMENSION = new Dimension(400, 140);
	/** Logger */
	private final JFrame parentFrame;
	private JPanel content;
	private JLabel nameLbl;
	private VmHandle handle;
	private JLabel nameValLbl;
	private JLabel domainLbl;
	private JLabel domainValLbl;
	private JLabel stateLbl;
	private JLabel stateValLbl;
	private JLabel progLbl;
	private JLabel progValLbl;

	private JLabel createLabel(String text) {
		JLabel lbl = new JLabel(text);
		lbl.setPreferredSize(new Dimension(300, 15));
		getContentPane().add(lbl);
		lbl.setForeground(Color.LIGHT_GRAY);
		return lbl;
	}

	public OsdFrame(JFrame parentFrame) {
		this.parentFrame = parentFrame;
		setUndecorated(true);
		content = new JPanel();
		SpringLayout layout = new SpringLayout();
		content.setLayout(layout);
		content.setBackground(Color.DARK_GRAY);
		setContentPane(content);
		nameLbl = createLabel("Name:");
		nameValLbl = createLabel("");
		domainLbl = createLabel("Domain:");
		domainValLbl = createLabel("");
		stateLbl = createLabel("State:");
		stateValLbl = createLabel("");
		progLbl = createLabel("Progress:");
		progValLbl = createLabel("");
		JLabel icon = new JLabel(IconManager.getInstance().getIcon("swiss_small.png"));
		content.add(icon);
		layout.putConstraint(SpringLayout.NORTH, icon, 3, SpringLayout.NORTH, content);
		layout.putConstraint(SpringLayout.WEST, icon, 3, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.NORTH, nameLbl, 20, SpringLayout.NORTH, content);
		layout.putConstraint(SpringLayout.WEST, nameLbl, LEFT_MARGIN, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.NORTH, domainLbl, 10, SpringLayout.SOUTH, nameLbl);
		layout.putConstraint(SpringLayout.WEST, domainLbl, LEFT_MARGIN, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.NORTH, stateLbl, 10, SpringLayout.SOUTH, domainLbl);
		layout.putConstraint(SpringLayout.WEST, stateLbl, LEFT_MARGIN, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.NORTH, progLbl, 10, SpringLayout.SOUTH, stateLbl);
		layout.putConstraint(SpringLayout.WEST, progLbl, LEFT_MARGIN, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.NORTH, nameValLbl, 20, SpringLayout.NORTH, content);
		layout.putConstraint(SpringLayout.WEST, nameValLbl, 130, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.NORTH, domainValLbl, 10, SpringLayout.SOUTH, nameLbl);
		layout.putConstraint(SpringLayout.WEST, domainValLbl, 130, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.NORTH, stateValLbl, 10, SpringLayout.SOUTH, domainLbl);
		layout.putConstraint(SpringLayout.WEST, stateValLbl, 130, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.NORTH, progValLbl, 10, SpringLayout.SOUTH, stateLbl);
		layout.putConstraint(SpringLayout.WEST, progValLbl, 130, SpringLayout.WEST, content);
	}

	public void displayOsd() {
		if (handle == null) {
			return;
		}
		// sizing, position
		setAlwaysOnTop(true);
		setPreferredSize(OSD_DIMENSION);
		// content.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
		pack();
		Rectangle bnd = parentFrame.getBounds();
		// above / centered
		setLocation((int) bnd.getX() + (bnd.width - getWidth()) / 2, (int) bnd.getY() + 50);
		// side / low
		// setLocation((int) bnd.getX() , (int)
		// (bnd.getY()+bnd.getHeight()-getHeight()-100));
		setVisible(true);
	}

	public void setVmHandle(VmHandle handle, IVmMonitor vmMon) {
		this.handle = handle;
		if (handle != null) {
			nameValLbl.setText(vmMon.getVmName(handle));
			CubeClassification classification = vmMon.getVmClassification(handle);
			domainValLbl.setText(String.format("%s (%s)", vmMon.getVmDomain(handle), classification));
			stateValLbl.setText(String.format("%s", vmMon.getVmState(handle)));
			int progress = vmMon.getVmProgress(handle);
			String message = vmMon.getVmProgressMessage(handle);
			if (message == null) {
				message = "";
			}
			if (progress > 0 && progress < 100) {
				progLbl.setText("Progress:");
				progValLbl.setText(String.format("%s (%d%%)", message, progress));
			} else {
				progLbl.setText("");
				progValLbl.setText("");
			}
			//
			content.setBorder(BorderFactory.createLineBorder(BorderColorProvider.getBackgroundColor(classification), 5));
		}
	}
}
