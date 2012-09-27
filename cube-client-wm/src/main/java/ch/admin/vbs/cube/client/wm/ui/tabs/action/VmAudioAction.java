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

import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.ui.dialog.AudioDialog;
import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.core.vm.VmAudioControl;

public class VmAudioAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private VmHandle vmHandle;
	private String vmName;

	public VmAudioAction(final VmHandle vmHandle, final String vmName) {
		super(I18nBundleProvider.getBundle().getString("vm.action.volume.text"));
		if (vmHandle == null) {
			throw new NullPointerException("vmHandle should not be null");
		}
		this.vmHandle = vmHandle;
		this.vmName = vmName;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final String vmId = vmHandle.getVmId();
				final VmAudioControl vmc = new VmAudioControl();
				final AudioDialog dial = new AudioDialog(null, vmId, vmc, vmName);
				dial.displayWizard();
			}
		});
	}
}