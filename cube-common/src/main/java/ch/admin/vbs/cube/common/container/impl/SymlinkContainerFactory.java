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

package ch.admin.vbs.cube.common.container.impl;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.ContainerException;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;
import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;

/**
 * This class implements IContainerFactory. It use unix filesystem symlinks to
 * simulate creating, mounting and unmounting containers. This class do not use
 * Perl scripts to perform the necessary system operations (unlike
 * DmcryptContainerFactory).
 * 
 * This implementation is mostly for test and development purpose (faster than
 * using encrypted containers)
 */
public class SymlinkContainerFactory implements IContainerFactory {
	private ScriptUtil su = new ScriptUtil();

	@Override
	public void createContainer(Container container, EncryptionKey key) throws ContainerException {
		try {
			ShellUtil shell = su.execute("mkdir", "-p", container.getContainerFile().getAbsolutePath());
			if (shell.getExitValue() != 0) {
				throw new ShellUtilException("Script returned an error [" + shell.getExitValue() + "]");
			}
		} catch (Exception e) {
			throw new ContainerException("Failed to execute script", e);
		}
	}

	@Override
	public void deleteContainer(Container container) throws ContainerException {
		// cleanup mountpoint
		try {
			su.execute("rm", "-rf", container.getContainerFile().getAbsolutePath());
		} catch (ShellUtilException e) {
			throw new ContainerException("Failed to delete container from filesystem", e);
		}
	}

	@Override
	public void mountContainer(Container container, EncryptionKey key) throws ContainerException {
		// prepare mountpoint
		if (container.getMountpoint() == null)
			throw new ContainerException("Mountpoint property is null");
		if (container.getMountpoint().exists())
			throw new ContainerException("Mountpoint [" + container.getMountpoint().getAbsolutePath() + "] already exists");
		// use shell script to mount container
		try {
			su.execute("ln", "-s", container.getContainerFile().getAbsolutePath(), container.getMountpoint().getAbsolutePath());
		} catch (Exception e) {
			throw new ContainerException("Failed to execute script", e);
		}
	}

	@Override
	public void unmountContainer(Container container) throws ContainerException {
		try {
			su.execute("rm", "-f", container.getMountpoint().getAbsolutePath());
		} catch (Exception e) {
			throw new ContainerException("Failed to execute script", e);
		}
	}
}
