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

package ch.admin.vbs.cube.client.wm.ui.tabs.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import ch.admin.vbs.cube.client.wm.client.IUserInterface;
import ch.admin.vbs.cube.client.wm.ui.wm.WindowManager;

/**
 * 
 */
public class CubeWifiAction extends AbstractAction {
	private static final long serialVersionUID = 0;
	private WindowManager wm;

	/**
	 * Constructs the cube lock action which will invoke all listeners, when the
	 * action has been executed.
	 * @param cubeUI 
	 * @param b 
	 */
	public CubeWifiAction(String text, IUserInterface userUI) {
		super(text);
		this.wm = (WindowManager)userUI;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				wm.openDebug();
			}
		});
	}
}
