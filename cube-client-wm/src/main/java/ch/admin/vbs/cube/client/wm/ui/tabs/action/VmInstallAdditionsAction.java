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

import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.client.wm.utils.IconManager;

/**
 * 
 * 
 */
public class VmInstallAdditionsAction extends VmAbstractAction {
	private static final long serialVersionUID = 3185872544856252979L;

	/**
	 * Constructs the vm start action which will invoke all listeners, when the
	 * action has been executed.
	 * 
	 * @param vmId
	 *            vmId of the virtual machine
	 */
	public VmInstallAdditionsAction(VmHandle h) {
		super(I18nBundleProvider.getBundle().getString("vm.action.guestadditions.text"), IconManager.getInstance().getIcon("vbox_icon16.png"), h);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.newwm.action.VmAbstractAction#exec(java.awt.event.ActionEvent
	 * )
	 */
	@Override
	public void exec(ActionEvent actionEvent) {
		for (IVmActionListener listener : getVmActionListeners()) {
			listener.installGuestAdditions(getVmHandle());
		}
	}
}
