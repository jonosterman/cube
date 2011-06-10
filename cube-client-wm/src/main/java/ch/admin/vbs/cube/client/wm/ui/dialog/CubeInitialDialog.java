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

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.ICubeActionListener;
import ch.admin.vbs.cube.client.wm.utils.IconManager;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.swing.JideSplitButton;

/**
 * This class represent a lock message dialog. This dialog has no decoration,
 * only a message. Use display() to show the dialog.
 */
public class CubeInitialDialog extends CubeMessageDialog {
	private static final long serialVersionUID = 1L;
	private final ICubeActionListener cubeListener;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(CubeInitialDialog.class);

	/**
	 * Creates a lock message dialog with the given message.
	 * 
	 * @param owner
	 *            the owner of the dialog, which the dialog belong to
	 * @param message
	 *            dialog message text
	 */
	public CubeInitialDialog(JFrame owner, String message, ICubeActionListener cubeListener) {
		super(owner, message);
		this.cubeListener = cubeListener;
		setModal(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jidesoft.dialog.StandardDialog#createButtonPanel()
	 */
	@Override
	public ButtonPanel createButtonPanel() {
		ButtonPanel p = new ButtonPanel();
		JideSplitButton more1 = new JideSplitButton("More...");
		more1.setSelected(false);
		more1.setButtonStyle(JideSplitButton.TOOLBOX_STYLE);
		p.add(more1);
		more1.setEnabled(true);
		more1.setButtonEnabled(true);
		more1.setAlwaysDropdown(true);
		more1.setForeground(Color.GRAY);
		more1.add(new AbstractAction("Shutdown", IconManager.getInstance().getIcon("shutdown_icon24.png")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							cubeListener.shutdownMachine();
						} catch (Exception e2) {
							LOG.error("Failed to shutdown machine", e2);
						}
					}
				}).start();
			}
		});
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		return p;
	}
}
