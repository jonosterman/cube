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

package ch.admin.vbs.cube.core.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeCommonProperties;
import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.ContainerException;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.impl.KeyGenerator;
import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;

public class TransferContainerFactory {
	private static final String TRANSFER_CONTAINER_EXTENSION = ".tr.data";
	private static final String TRANSFER_MOUNTPOINT_EXTENSION = ".tr.open";
	private static final long MEGA_50 = 1024 * 1024 * 50;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(TransferContainerFactory.class);
	private IContainerFactory containerFactory;
	private Executor exec = Executors.newCachedThreadPool();

	/**
	 * Initialize a one-time encrypted container to store temporary data. -
	 * generate one-time random key <br/>
	 * - create temporary transfer container <br/>
	 * - mount temporary transfer container <br/>
	 * - shred one-time random key <br/>
	 * 
	 * @param session
	 * @return
	 * @throws IOException
	 * @throws ContainerException
	 */
	public Container initTransfer(IIdentityToken id) throws IOException, ContainerException {
		LOG.debug("initTransferContainer({})", id.getSubjectName());
		// create one-time key. This key will be deleted (shred) after the
		// container has been mounted.
		final File tempKey = File.createTempFile("SessionKeyTemp", ".tmp");
		FileOutputStream keyFos = new FileOutputStream(tempKey);
		KeyGenerator.generateKey(4096, keyFos);
		keyFos.close();
		// create container object
		Container trCnt = new Container();
		trCnt.setContainerFile(new File(new File(CubeCommonProperties.getProperty("cube.containers.dir")), id.getUuidHash() + TRANSFER_CONTAINER_EXTENSION));
		trCnt.setId(id.getUuidHash());
		EncryptionKey trKey = new EncryptionKey(null, tempKey);
		trCnt.setMountpoint(new File(CubeCommonProperties.getProperty("cube.mountpoints.dir"), trCnt.getId() + TRANSFER_MOUNTPOINT_EXTENSION));
		trCnt.setSize(MEGA_50);
		// unmount and delete older container if present.
		LOG.debug("Unmount old transfer container [{}]", trCnt.getMountpoint().getAbsolutePath());
		try {
			// does not check if the mount point exists, since even if it does
			// not exists, a lock file may to be deleted.
			containerFactory.unmountContainer(trCnt);
		} catch (Exception e) {
		}
		if (trCnt.getMountpoint().exists()) {
			try {
				trCnt.getMountpoint().delete();
			} catch (Exception e) {
			}
		}
		if (trCnt.getContainerFile().exists()) {
			LOG.debug("Delete old transfer container [{}].", trCnt.getContainerFile().getAbsolutePath());
			try {
				containerFactory.deleteContainer(trCnt);
			} catch (Exception e) { // nothing
			}
		}
		// create a new container and mount it
		LOG.debug("Create a new transfer container and mount it.");
		try {
			containerFactory.createContainer(trCnt, trKey);
			containerFactory.mountContainer(trCnt, trKey);
		} finally {
			// shred temporary clear-text key. Do it in another thread in order
			// to save 1 second during login.
			if (tempKey.exists()) {
				exec.execute(new Runnable() {
					@Override
					public void run() {
						long t0 = System.currentTimeMillis();
						ShellUtil su = new ShellUtil();
						try {
							su.run(null, ShellUtil.NO_TIMEOUT, "shred", "-u", tempKey.getAbsolutePath());
						} catch (ShellUtilException e) {
							LOG.error("Failed to shred temp key of transfer container", e);
						}
						LOG.debug("Key shreded in [{} ms].", System.currentTimeMillis() - t0);
					}
				});
			}
		}
		return trCnt;
	}

	public void disposeTransfer(Container cnt) throws ContainerException {
		if (cnt != null) {
			containerFactory.unmountContainer(cnt);
			containerFactory.deleteContainer(cnt);
		}
	}

	public void setContainerFactory(IContainerFactory containerFactory) {
		this.containerFactory = containerFactory;
	}
}
