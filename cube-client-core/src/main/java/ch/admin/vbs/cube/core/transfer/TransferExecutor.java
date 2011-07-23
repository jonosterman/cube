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

package ch.admin.vbs.cube.core.transfer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;
import ch.admin.vbs.cube.core.vm.Vm;

public class TransferExecutor {
	/** Logger */
	private static final Log LOG = LogFactory.getLog(TransferExecutor.class);

	public void transfer(RelativeFile filename, Vm srcvm, Vm dstvm) {
		if (srcvm != null && srcvm.getExportFolder().exists() && dstvm != null && dstvm.getImportFolder().exists()) {
			try {
				ScriptUtil script = new ScriptUtil();
				script.execute("./transfer-copy.pl", "--file", filename.getFile().getAbsolutePath(), "--dir", dstvm.getImportFolder().getAbsolutePath());
			} catch (ShellUtilException e) {
				LOG.error("Failed to cleanup export folder.");
			}
		}
	}

	public void cleanup(Vm vm) {
		if (vm != null && vm.getExportFolder().exists()) {
			try {
				ScriptUtil script = new ScriptUtil();
				script.execute("./transfer-cleanupfolder.pl", "--folder", vm.getExportFolder().getAbsolutePath());
			} catch (ShellUtilException e) {
				LOG.error("Failed to cleanup export folder.");
			}
		}
	}
}
