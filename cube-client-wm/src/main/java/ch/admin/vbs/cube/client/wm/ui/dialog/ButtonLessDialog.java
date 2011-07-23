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

import javax.swing.JFrame;

import com.jidesoft.dialog.ButtonPanel;

/**
 * This class represent a lock message dialog. This dialog has no decoration,
 * only a message. Use display() to show the dialog.
 */
public class ButtonLessDialog extends CubeMessageDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a lock message dialog with the given message.
	 * 
	 * @param owner
	 *            the owner of the dialog, which the dialog belong to
	 * @param message
	 *            dialog message text
	 */
	public ButtonLessDialog(JFrame owner, String message) {
		super(owner, message);
		setModal(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jidesoft.dialog.StandardDialog#createButtonPanel()
	 */
	@Override
	public ButtonPanel createButtonPanel() {
		return null;
	}
}
