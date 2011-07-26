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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import ch.admin.vbs.cube.client.wm.ui.CubeUI;
import ch.admin.vbs.cube.client.wm.utils.IconManager;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

/**
 * This class represent a default message dialog. Title and message have a
 * default text depending of the message typed. This means, that if no text was
 * set, inclusive null, the default text will be taken from the resource bundle.
 * Use display() to show the dialog.
 * 
 * 
 */
public abstract class CubeWizard extends StandardDialog {
	public static final String WIZARD_WINDOW_TITLE = "CubeWizard";
	/** Logger */
	private static final long serialVersionUID = 0L;
	private static final Dimension MINIMUM_DIALOG_SIZE = new Dimension(400, 247);
	private JPanel centerContentPanel;
	private JPanel leftContentPanel;
	private JPanel bannerPanel;
	private ButtonPanel buttonPanel;
	private JPanel contentPanel;
	private int buttonCount = 0;

	/**
	 * Creates a message dialog with the given message, title and message type.
	 * 
	 * @param owner
	 *            the owner of the dialog, which the dialog belong to
	 * @param title
	 *            the title of the dialog
	 */
	public CubeWizard(JFrame owner) {
		super(owner, true);
		setTitle(WIZARD_WINDOW_TITLE);
		setUndecorated(true);
	}

	private void buildUI() {
		// header
		bannerPanel = new JPanel();
		SpringLayout bannerLayout = new SpringLayout();
		bannerPanel.setLayout(bannerLayout);
		JLabel bannerBg = new JLabel(IconManager.getInstance().getIcon("cube_banner_x58.png"));
		bannerLayout.putConstraint(SpringLayout.NORTH, bannerBg, 0, SpringLayout.NORTH, bannerPanel);
		bannerLayout.putConstraint(SpringLayout.SOUTH, bannerPanel, 58, SpringLayout.NORTH, bannerPanel);
		bannerLayout.putConstraint(SpringLayout.WEST, bannerBg, 0, SpringLayout.WEST, bannerPanel);
		bannerPanel.add(bannerBg);
		// left content
		leftContentPanel = new JPanel(); // reserved
		// content
		centerContentPanel = createCenterContentPanel(); // to be extended by
		// sub-classes
		// buttons
		buttonPanel = new ButtonPanel(SwingConstants.RIGHT); // empty now
		// general layout
		contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(new JSeparator(), BorderLayout.NORTH);
		if (buttonCount > 0) {
			contentPanel.add(new JSeparator(), BorderLayout.SOUTH);
		}
		contentPanel.add(leftContentPanel, BorderLayout.WEST);
		contentPanel.add(centerContentPanel, BorderLayout.CENTER);
		setMinimumSize(MINIMUM_DIALOG_SIZE);
		JPanel dialog = (JPanel) getContentPane();
		dialog.setBorder(BorderFactory.createEtchedBorder());
	}

	protected abstract JPanel createCenterContentPanel();

	/**
	 * Shows the message dialog.
	 */
	public void displayWizard() {
		// set dialog in the center of the screen with a default
		// size
		buildUI();
		pack();
		// center
		Point leftUpperCorner = new Point(//
				CubeUI.getDefaultScreenBounds().x + (CubeUI.getDefaultScreenBounds().width / 2),//
				CubeUI.getDefaultScreenBounds().y + (CubeUI.getDefaultScreenBounds().height) / 2);
		leftUpperCorner.translate(-getWidth() / 2, -getHeight() / 2);
		setLocation(Math.max(0, leftUpperCorner.x), Math.max(0, leftUpperCorner.y));
		// dispose dialog when no more visible
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		// show dialog
		setVisible(true);
	}

	public void hideWizard() {
		setVisible(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jidesoft.dialog.StandardDialog#createBannerPanel()
	 */
	@Override
	public JComponent createBannerPanel() {
		return bannerPanel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jidesoft.dialog.StandardDialog#createButtonPanel()
	 */
	@Override
	public ButtonPanel createButtonPanel() {
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
		return buttonPanel;
	}

	public JButton addWizardAction(Action action) {
		return addWizardAction(action, false);
	}

	public JButton addWizardAction(Action action, boolean isDefault) {
		JButton button = new JButton(action);
		buttonPanel.add(button);
		if (isDefault) {
			getRootPane().setDefaultButton(button);
		}
		buttonCount++;
		return button;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jidesoft.dialog.StandardDialog#createContentPanel()
	 */
	@Override
	public JComponent createContentPanel() {
		return contentPanel;
	}
}
