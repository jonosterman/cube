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

import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.vm.Vm;

/**
 * The listener interface for receiving the close and fileTransfer event from
 * the {@link FileTransferWizard}. The class that is interested in the close and
 * fileTransfer event implements this interface.
 * 
 * 
 */
public interface FileTransferWizardListener {
	void cancelTransfer(RelativeFile filename, Vm sourceVm);

	/**
	 * Invoked when the user transfers a file from one vm to another.
	 * 
	 * @param fileName
	 *            the file which should be transferred
	 * @param vmIdFrom
	 *            the vm from which the file will be exported
	 * @param vmIdTo
	 *            the vm to which the file will be imported
	 */
	void fileTransfer(RelativeFile filename, Vm sourceVm, Vm destinationVm);
}
