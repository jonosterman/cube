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

package ch.admin.vbs.cube.client.wm.ui.tabs;

import javax.swing.ImageIcon;

import ch.admin.vbs.cube.client.wm.utils.IconManager;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmStatus;

/**
 * Provide status icon based on VM state and progress
 */
public class TabIconProvider {
	private static final int STAGING_PROGRESS_25 = 25;
	private static final int STAGING_PROGRESS_50 = 50;
	private static final int STAGING_PROGRESS_75 = 75;

	/**
	 * @param vm
	 *            the vm for which the icon is asked for
	 * @return Returns the status image icon for the given vm, depending on its
	 *         state.
	 */
	public static final ImageIcon getStatusIcon(Vm vm) {
		ImageIcon icon = null;
		if (vm != null) {
			VmStatus vmStatus = vm.getVmStatus();
			switch (vmStatus) {
			case RUNNING:
				icon = IconManager.getInstance().getIcon("vm_running.png");
				break;
			case STARTING:
			case STOPPING:
				icon = IconManager.getInstance().getIcon("vm_processing.gif");
				break;
			case STAGABLE:
				icon = IconManager.getInstance().getIcon("vm_stagable.png");
				break;
			case STAGING:
				int stagingProgress = vm.getProgress();
				if (stagingProgress >= STAGING_PROGRESS_75) {
					icon = IconManager.getInstance().getIcon("vm_staging_75.gif");
				} else if (stagingProgress >= STAGING_PROGRESS_50) {
					icon = IconManager.getInstance().getIcon("vm_staging_50.gif");
				} else if (stagingProgress >= STAGING_PROGRESS_25) {
					icon = IconManager.getInstance().getIcon("vm_staging_25.gif");
				} else {
					icon = IconManager.getInstance().getIcon("vm_staging_0.gif");
				}
				break;
			case STOPPED:
				icon = IconManager.getInstance().getIcon("vm_stopped.png");
				break;
			case ERROR:
			case UNKNOWN:
			default:
				icon = IconManager.getInstance().getIcon("vm_error.png");
				break;
			}
		}
		return icon;
	}

	/**
	 * @param vm
	 *            the vm for which the icon is asked for
	 * @return Returns the status image icon for the given vm, depending on its
	 *         state.
	 */
	public static final ImageIcon getStatusIcon(VmStatus vmStatus) {
		ImageIcon icon = null;
		if (vmStatus != null) {
			switch (vmStatus) {
			case RUNNING:
				icon = IconManager.getInstance().getIcon("vm_running.png");
				break;
			case STARTING:
			case STOPPING:
				icon = IconManager.getInstance().getIcon("vm_processing.gif");
				break;
			case STAGABLE:
				icon = IconManager.getInstance().getIcon("vm_stagable.png");
				break;
			case STAGING:
				icon = IconManager.getInstance().getIcon("vm_staging_0.gif");
				break;
			case STOPPED:
				icon = IconManager.getInstance().getIcon("vm_stopped.png");
				break;
			case ERROR:
			case UNKNOWN:
			default:
				icon = IconManager.getInstance().getIcon("vm_error.png");
				break;
			}
		}
		return icon;
	}
}
