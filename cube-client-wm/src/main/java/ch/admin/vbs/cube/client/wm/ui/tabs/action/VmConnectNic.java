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
import java.util.HashMap;

import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.core.vm.NicOption;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

/**
 * 
 */
public class VmConnectNic extends VmAbstractAction {
	private static final long serialVersionUID = 0L;
	private final String nic;
	private final HashMap<String, String> selectedNics;

	/**
	 * @param selectedNics
	 * 
	 */
	public VmConnectNic(VmHandle handle, String nic, HashMap<String, String> selectedNics) {
		super(VBoxProduct.ORIGINAL_NETWORK_CONFIG.equals(nic) ? I18nBundleProvider.getBundle().getString("vm.action.connectnic.original.text") : nic, null, handle);
		this.nic = nic;
		this.selectedNics = selectedNics;
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
		selectedNics.put(super.getVmHandle().getVmId(), nic);
		for (IVmActionListener listener : getVmActionListeners()) {
			listener.connectNic(getVmHandle(), new NicOption(nic));
		}
	}
}
