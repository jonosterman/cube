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

package ch.admin.vbs.cube.client.wm.ui.tabs.action;

import java.awt.event.ActionEvent;

import ch.admin.vbs.cube.client.wm.client.ICubeActionListener;
import ch.admin.vbs.cube.client.wm.utils.IconManager;
import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;

/**
 * This class handles the shutdown action for the physical computer. When the
 * action is invoked, all listeners will be informed.
 * 
 * 
 * 
 */
public class CubeShutdownAction extends CubeAbstractAction {
	private static final long serialVersionUID = 5233135459540289677L;

	/**
	 * Constructs the cube shutdown action which will invoke all listeners, when
	 * the action has been executed.
	 */
	public CubeShutdownAction() {
		super(I18nBundleProvider.getBundle().getString("cube.action.shutdown.text"), IconManager.getInstance().getIcon("shutdown_icon16.png"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.cube.client.wm.se.action.cube.CubeAbstractAction#exec(java
	 * .awt.event.ActionEvent)
	 */
	@Override
	public void exec(ActionEvent actionEvent) {
		for (ICubeActionListener listener : getCubeActionListeners()) {
			listener.shutdownMachine();
		}
	}
}
