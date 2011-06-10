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

package ch.admin.vbs.cube.core.transfer;

import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.vm.Vm;

/**
 * Listens for files being exported.
 */
public interface TransferListener {
	/**
	 * Notifies that a file is being exported.
	 * 
	 * @param vm
	 *            the virtual machine from which the file is being exported.
	 * @param fileName
	 *            the name of the file being exported.
	 */
	void notifyFileExport(Vm vm, RelativeFile fileName);
}
