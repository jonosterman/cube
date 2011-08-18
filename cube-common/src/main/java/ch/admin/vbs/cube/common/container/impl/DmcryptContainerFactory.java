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

package ch.admin.vbs.cube.common.container.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeCommonProperties;
import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.ContainerException;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.container.SizeFormatUtil;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;
import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;

/**
 * This class implements IContainerFactory. It use DM-Crypt tools (Linux) to
 * create, mount and unmount encrypted containers. This class use several Perl
 * scripts to perform the necessary system operations. The use of scripts ensure
 * performance (command line execution is slow in Java) and flexibility.
 * 
 * All cube scripts are located in a central directory (default
 * /opt/cube/client/scripts) and some need 'sudo' rights (see documentation).
 * 
 * A file-based lock has been implemented in Perl script. The lock holds
 * references to several resources used to mount the file. (because it is
 * impossible to find them afterward since 'losetup' trunk its output). Mounting
 * volume that has been already mounted (and a lock is present) will fail with a
 * special code '65' at the script level and we handle it by performing an
 * unmout-remount sequence. The lock file is remove when the volume has been
 * successfully unmounted.
 * 
 */
public class DmcryptContainerFactory implements IContainerFactory {
	private static final int EXIT_CODE_WHEN_LOCK_FILE_IS_PRESENT = 65;
	private static final int EXIT_CODE_WHEN_THERE_IS_NO_MORE_FREE_LOOP_DEVICES = 45;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(DmcryptContainerFactory.class);
	private ScriptUtil su = new ScriptUtil();

	/** Cleanup remaining open containers. */
	public static void cleanup() {
		try {
			File varDir = new File(CubeCommonProperties.getProperty("cube.scripts.dir") + "/../var/");
			if (!varDir.exists()) {
				LOG.debug("Cleanup skipped. Directory [{}] does not exist", varDir.getCanonicalPath());
				return;
			}
			LOG.debug("Cleanup remaining containers.");
			for (File lock : varDir.listFiles()) {
				// remove unused locks
				if (lock.getName().endsWith(".last")) {
				} else {
					LOG.debug("Try to unmount container [{}]", lock.getName());
					Properties p = new Properties();
					try {
						FileInputStream is = new FileInputStream(lock);
						p.load(is);
						is.close();
						//
						Container c = new Container();
						c.setContainerFile(new File(p.getProperty("file")));
						c.setMountpoint(new File(p.getProperty("mountpoint")));
						c.setId("n/a");
						DmcryptContainerFactory f = new DmcryptContainerFactory();
						f.unmountContainer(c);
					} catch (Exception e) {
						LOG.error("Failed to unlock [" + lock.getAbsolutePath() + "]", e);
					} finally {
						if (lock.exists()) {
							try {
								lock.delete();
							} catch (Exception e) {
								LOG.error("Failed to unlock [" + lock.getAbsolutePath() + "]", e);
							}
						}
					}
				}
			}
			// System.out.println("--------------");
			// InputStream is =
			// Runtime.getRuntime().exec("mount | grep 'temp-vol'").getInputStream();
			// BufferedReader br = new BufferedReader(new
			// InputStreamReader(is));
			// String line = br.readLine();
			// while (line != null) {
			// System.out.println(line);
			// line = br.readLine();
			// }
			// System.out.println("--------------");
		} catch (Exception e) {
			LOG.error("Failed to cleanup", e);
		}
	}

	@Override
	public void createContainer(Container container, EncryptionKey key) throws ContainerException {
		try {
			LOG.debug("Create container [{}] (could take some time)", SizeFormatUtil.format(container.getSize()));
			ShellUtil shell = su.execute("sudo", "./dmcrypt-create-container.pl", "-f", container.getContainerFile().getAbsolutePath(), "-k", key.getFile()
					.getAbsolutePath(), "-s", "" + container.getSize());
			if (shell.getExitValue() != 0) {
				// Handle sudo errors with a custom log message in order to help
				// the administrator to find that its sudo configuration smells.
				if (shell.getStandardError().indexOf("sudo: no tty present and no askpass program specified") >= 0) {
					LOG.error("SUDO is not correctly configured: It ask a password (interactive) in order to execute the script. Edit your sudoer file (-> visudo).");
					throw new ContainerException("Script returned an error because SUDO is not configured correctly");
				} else {
					throwErrorWithShellOutput(shell, "Script returned an error [" + shell.getExitValue() + "]");
				}
			}
		} catch (ContainerException e) {
			throw e;
		} catch (Exception e) {
			throw new ContainerException("Failed to execute script", e);
		}
	}

	@Override
	public void deleteContainer(Container container) throws ContainerException {
		try {
			LOG.debug("Delete container..");
			ShellUtil s = su.execute("sudo", "./dmcrypt-delete-container.pl", "-f", container.getContainerFile().getAbsolutePath(), "-m", container
					.getMountpoint().getAbsolutePath());
			if (s.getExitValue() != 0) {
				throwErrorWithShellOutput(s, "script returned a non-zero code [" + s.getExitValue() + "]");
			}
		} catch (ContainerException e) {
			throw e;
		} catch (ShellUtilException e) {
			throw new ContainerException("Could not delete container [" + container + "]", e);
		}
	}

	@Override
	public void mountContainer(Container container, EncryptionKey key) throws ContainerException {
		try {
			ShellUtil s = su.execute("sudo", "./dmcrypt-mount-container.pl", "-f", container.getContainerFile().getAbsolutePath(), "-k", key.getFile()
					.getAbsolutePath(), "-m", container.getMountpoint().getAbsolutePath());
			if (s.getExitValue() == 0) {
				LOG.debug("Container successfully mounted");
			} else if (s.getExitValue() == EXIT_CODE_WHEN_THERE_IS_NO_MORE_FREE_LOOP_DEVICES) {
				throw new ContainerException(
						"Failed to mount container. There is no more free loop devices. add 'options loop max_loop=256' to /etc/modprobe.d/cube.conf");
			} else if (s.getExitValue() == EXIT_CODE_WHEN_LOCK_FILE_IS_PRESENT) {
				/*
				 * lock file is present. perhaps it is mounted or was not
				 * cleannly unmounted. Since it is not clear, we perform an
				 * unmount-remount sequence.
				 */
				unmountContainer(container);
				s = su.execute("sudo", "./dmcrypt-mount-container.pl", "-f", container.getContainerFile().getAbsolutePath(), "-k", key.getFile()
						.getAbsolutePath(), "-m", container.getMountpoint().getAbsolutePath());
				if (s.getExitValue() == 0) {
					LOG.debug("Container successfully mounted (but unmount/remount was required)");
				} else if (s.getExitValue() == EXIT_CODE_WHEN_LOCK_FILE_IS_PRESENT) {
					/*
					 * something is odd. The lock is still there, even after we
					 * perfomred an unmount. freaky.
					 */
					throwErrorWithShellOutput(s, "Mounting container failed [" + s.getExitValue() + "]");
				} else {
					/* We failed to remove the lock. Even more freaky. */
					throwErrorWithShellOutput(s, "Mounting container failed [" + s.getExitValue() + "]");
				}
			} else {
				throwErrorWithShellOutput(s, "Mounting container failed [" + s.getExitValue() + "]");
			}
		} catch (ContainerException e) {
			throw e;
		} catch (Exception e) {
			throw new ContainerException("Could not mount container [" + container + "]", e);
		}
	}

	private void throwErrorWithShellOutput(ShellUtil s, String string) throws ContainerException {
		LOG.error("----------------------------" + "STDOUT :\n" + s.getStandardOutput() + "\nSTDERR :\n" + s.getStandardError());
		LOG.error("----------------------------");
		throw new ContainerException(string);
	}

	@Override
	public void unmountContainer(Container container) throws ContainerException {
		try {
			ShellUtil s = su.execute("sudo", "./dmcrypt-unmount-container.pl", "-f", container.getContainerFile().getAbsolutePath(), "-m", container
					.getMountpoint().getAbsolutePath());
			if (s.getExitValue() != 0) {
				throwErrorWithShellOutput(s, "script returned a non-zero code [" + s.getExitValue() + "]");
			} else {
				LOG.debug("Container [" + container.getContainerFile() + "] unmounted");
			}
		} catch (ContainerException e) {
			throw e;
		} catch (Exception e) {
			throw new ContainerException("Could not unmount container [" + container + "]", e);
		}
	}
}
