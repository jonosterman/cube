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

package ch.admin.vbs.cube.common.keyring;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.keyring.impl.KeyringException;
import ch.admin.vbs.cube.common.shell.ShellUtil;

/**
 * EncryptionKey is a symmetric key file that can be used by external programs
 * (dmcrypt). You can get a key through the IKeyring class. Since this key is
 * decrypted on the disk, you HAVE TO shred it as soon as possible!
 * 
 * 
 * 
 */
public class EncryptionKey {
	private static final Logger LOG = LoggerFactory.getLogger(EncryptionKey.class);

	/** States */
	private enum KeyState {
		NOT_INITIALIZED, DECRYPTED, SHREDED
	}

	private final String id;
	private final File file;
	private KeyState state = KeyState.NOT_INITIALIZED;

	/**
	 * Create a key object.
	 * 
	 * @param id
	 *            unique ID
	 * @param file
	 *            file containing decrypted key
	 */
	public EncryptionKey(String id, File file) {
		this.id = id;
		this.file = file;
		state = KeyState.DECRYPTED;
	}

	public String getId() {
		return id;
	}

	/**
	 * @throws KeyringException
	 * @throw KeyringException key file is not available (probably already
	 *        shreded)
	 */
	public File getFile() throws KeyringException {
		if (state != KeyState.DECRYPTED) {
			throw new KeyringException("Key is not in a readable state [" + state + "]");
		}
		return file;
	}

	/**
	 * Shred the file containing the key (using command line 'shred' tool).
	 */
	public void shred() {
		try {
			if (file != null && file.exists() && state == KeyState.DECRYPTED) {
				file.setWritable(true);
				ShellUtil su = new ShellUtil();
				su.run(null, ShellUtil.NO_TIMEOUT, "shred", "-u", file.getAbsolutePath());
			} else {
				LOG.debug("Key not shreded [{}]", state);
			}
		} catch (Exception e) {
			LOG.error("failed to shred data", e);
		}
		state = KeyState.SHREDED;
	}

	public KeyState getState() {
		return state;
	}
}
