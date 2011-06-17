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

package ch.admin.vbs.cube.common.keyring.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeCommonProperties;
import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.common.UuidGenerator;
import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.ContainerException;
import ch.admin.vbs.cube.common.container.ContainerFactoryProvider;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.crypto.Base64;
import ch.admin.vbs.cube.common.crypto.HashUtil;
import ch.admin.vbs.cube.common.crypto.RSAEncryptUtil;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.IIdentityToken.KeyType;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.common.keyring.SafeFile;

/**
 * This keyring implements a IKeyring following Cube specifications.
 */
public class CubeKeyring implements IKeyring {
	private static final String KEYID_PREFIX = "KEY##";
	// directories
	private static final String KEYRING_MOUNTPOINTS_DIR_PROP = "cube.mountpoints.dir";
	private static final String KEYRING_CONTAINERS_DIR_PROP = "cube.containers.dir";
	private static final String KEYRING_KEYS_DIR_PROP = "cube.keys.dir";
	// key sizes
	private static final String KEYRING_DEFAULT_KEY_BIT_SIZE_PROP = "keyring.defaultKeyBitSize";
	private static final String KEYRING_DEFAULT_SIZE_PROP = "keyring.defaultSize";
	// file extensions
	public static final String KEYRING_KEY_FILEEXTENSION = ".keyring.key";
	public static final String KEYRING_CONTAINER_FILEEXTENSION = ".keyring.data";
	public static final String KEYRING_MONTPOINT_FILEEXTENSION = ".keyring.open";
	public static final String KEYRING_SUBKEY_FILEEXTENSION = ".key";
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(CubeKeyring.class);
	private IContainerFactory containerFactory;
	private Container keyringContainer;
	private File safeDir;
	private IIdentityToken id;
	private DataFile dfu = new DataFile();
	private DataStore ds = new DataStore();

	public CubeKeyring() {
		// initialize container factory
		try {
			containerFactory = ContainerFactoryProvider.getFactory();
		} catch (Exception e) {
			LOG.error("Failed to init container factory", e);
		}
	}

	public CubeKeyring(IContainerFactory factory) {
		this.containerFactory = factory;
	}

	@Override
	public void open(IIdentityToken id, File safeDir) throws KeyringException {
		this.id = id;
		this.safeDir = safeDir;
		// try {
		LOG.debug("Open keyring of user [" + id.getSubjectName() + "]");
		// define where key, container and mount-point are located in the
		// filesystem (default: /opt/cube/var/keyrings). User Uuid-hash is
		// inserted in this path to get user specific paths.
		File encryptedKeyringKey = new File(//
				new File(CubeCommonProperties.getProperty(KEYRING_KEYS_DIR_PROP)), //
				id.getUuidHash() + KEYRING_KEY_FILEEXTENSION);
		File keyringContainerFile = new File(//
				new File(CubeCommonProperties.getProperty(KEYRING_CONTAINERS_DIR_PROP)), //
				id.getUuidHash() + KEYRING_CONTAINER_FILEEXTENSION);
		File keyringMountpoint = new File(//
				CubeCommonProperties.getProperty(KEYRING_MOUNTPOINTS_DIR_PROP), //
				id.getUuidHash() + KEYRING_MONTPOINT_FILEEXTENSION);
		// prepare keyring's container object
		keyringContainer = new Container();
		keyringContainer.setContainerFile(keyringContainerFile);
		keyringContainer.setId(id.getUuidHash());
		keyringContainer.setMountpoint(keyringMountpoint);
		ds.setStoreDirectory(keyringMountpoint);
		// decryptedKeyringKeyFile WILL contains the deciphered keyring main
		// key. This key MUST not be stored anywhere, since it would be easy
		// for an attacker to find it, even after being deleted. We used to
		// save
		// it on an encrypted partition (transfer container).
		SafeFile decryptedKeyringKeyFile = new SafeFile(safeDir, UuidGenerator.generate());
		EncryptionKey key = new EncryptionKey("keyring", decryptedKeyringKeyFile);
		// try to re-used key, create a new one when none are present. Abort
		// if an existing key is present but is unusable. Avoid a fail-safe
		// approach where we re-generate a new key if the old failed,
		// because an attacker will be able to use it to delete legitim user
		// content.
		if (encryptedKeyringKey.exists()) {
			try {
				decryptKey(encryptedKeyringKey, decryptedKeyringKeyFile, id);
				if (decryptedKeyringKeyFile.exists() && decryptedKeyringKeyFile.length() > 0) {
					LOG.debug("re-use existing keyring's key (encrypted)[{}] (clear[{}])", encryptedKeyringKey.getAbsolutePath(),
							decryptedKeyringKeyFile.getAbsoluteFile());
				} else {
					throw new KeyringException(String.format("Failed to re-use keyring's key (encrypted)[%s] (clear[%s])",
							encryptedKeyringKey.getAbsolutePath(), decryptedKeyringKeyFile.getAbsoluteFile()));
				}
			} catch (Exception e) {
				throw new KeyringException(String.format("Failed to open keyring's key [%s].", encryptedKeyringKey.getAbsolutePath()), e);
			}
		} else {
			try {
				// No valid keyring's key is present on the disk. generate a
				// new one
				LOG.info("Create new keyring's key (encrypted)[{}]", encryptedKeyringKey.getAbsolutePath());
				// generate new random key, save it, encrypted, in a file
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				KeyGenerator.generateKey(Integer.parseInt(CubeCommonProperties.getProperty(KEYRING_DEFAULT_KEY_BIT_SIZE_PROP)), out);
				dfu.writeFile(decryptedKeyringKeyFile, out.toByteArray());
				// encrypt key with user's public key (think: smart-card)
				byte[] encKeyringKeyData = RSAEncryptUtil.encrypt(out.toByteArray(), id.getPublickey(KeyType.ENCIPHERMENT));
				// save encrypted key in a file
				dfu.writeFile(encryptedKeyringKey, encKeyringKeyData);
			} catch (Exception e) {
				throw new KeyringException(String.format("Failed to create keyring's key [%s].", encryptedKeyringKey.getAbsolutePath()), e);
			}
		}
		// re-use or create an new container for keyring
		if (keyringContainerFile.exists()) {
			// try to open existing keyring
			try {
				// it should not be mounted yet, but if something goes wrong in
				// a
				// previous user session, it still can be mounted. So we try to
				// unmount it, just in case. We could let it and skip mount, but
				// we will never be sure it has been mounted correctly with the
				// right key, etc. so we prefer clean it up and re-mount it
				// again.
				try {
					// does not check if the mount point exists, since even if
					// it does
					// not exists, a lock file may to be deleted.
					containerFactory.unmountContainer(keyringContainer);
				} catch (Exception e) {
				}
				if (keyringContainer.getMountpoint().exists()) {
					try {
						keyringContainer.getMountpoint().delete();
					} catch (Exception e) {
					}
				}
				try {
					// open container
					LOG.debug("Open keyring: container[{}]  key[{}]", keyringContainer.getContainerFile().getAbsolutePath(), keyringContainer.getMountpoint()
							.getAbsolutePath());
					containerFactory.mountContainer(keyringContainer, key);
				} finally {
					// shred & delete decrypted key
					key.shred();
				}
			} catch (Exception e) {
				throw new KeyringException(String.format("Failed to open keyring's key [%s].", keyringContainer.getContainerFile().getAbsolutePath()), e);
			}
		} else {
			try {
				// create new container
				keyringContainer.setSize(Long.parseLong(CubeCommonProperties.getProperty(KEYRING_DEFAULT_SIZE_PROP)));
				LOG.debug("Create new keyring's container [{}] [{} MB]", keyringContainer.getContainerFile().getAbsolutePath(),
						keyringContainer.getSize() / 1024 / 1024);
				containerFactory.createContainer(keyringContainer, key);
				// and mount it
				try {
					// open container
					LOG.debug("Open keyring: container[{}]  key[{}]", keyringContainer.getContainerFile().getAbsolutePath(), keyringContainer.getMountpoint()
							.getAbsolutePath());
					containerFactory.mountContainer(keyringContainer, key);
				} finally {
					// shred & delete decrypted key
					key.shred();
				}
			} catch (Exception e) {
				throw new KeyringException(String.format("Failed to create keyring's [%s].", keyringContainer.getContainerFile().getAbsolutePath()), e);
			}
		}
		// System.exit(0);
		// // check if container & key exist
		// if (encryptedKeyringKey.exists() && keyringContainerFile.exists()) {
		// // use existing keyring & key
		// LOG.debug("Use existing keyring's key (encrypted)[{}]",
		// encryptedKeyringKey.getAbsolutePath());
		// // decrypt key
		// // (@TODO if key was encrypted with an older key (not valid
		// // anymore), we MUST re-encrypt it with the new one)
		// // (@TODO provide a way to request the password if needed)
		// dfu.writeFile(decryptedKeyringKeyFile,
		// RSAEncryptUtil.decrypt(dfu.readFile(encryptedKeyringKey),
		// id.getPrivatekey(KeyType.ENCIPHERMENT)));
		// // it should not be mounted, but if something goes wrong in a
		// // previous user session, it still can be mounted. So we try to
		// // unmount it, just in case. We could let it and skip mount, but
		// // we will never be sure it has been mounted correctly with the
		// // right key, etc. so we prefer clean it up and re-mount it
		// // again.
		// LOG.debug("Try to unmount keyring (just in case something was left open). before mounting it again.");
		// try {
		// containerFactory.unmountContainer(keyringContainer);
		// } catch (Exception e) {
		// LOG.error("Failed to unmount keyring", e);
		// }
		// } else {
		// // eventually delete old keyring container or key
		// if (encryptedKeyringKey.exists()) {
		// LOG.debug("Delete old keyring's key (associated container does not exist)");
		// encryptedKeyringKey.delete();
		// }
		// if (keyringContainerFile.exists()) {
		// LOG.debug("Delete old keyring's container (associated key does not exist)");
		// containerFactory.deleteContainer(keyringContainer);
		// }
		// // create a new keyring (key + container)
		// LOG.debug("Create new keyring's key (encrypted)[{}]",
		// encryptedKeyringKey.getAbsolutePath());
		// // generate new random key, save it, decrypted, in a file
		// ByteArrayOutputStream out = new ByteArrayOutputStream();
		// KeyGenerator.generateKey(Integer.parseInt(CubeCommonProperties.getProperty(KEYRING_DEFAULT_KEY_BIT_SIZE_PROP)),
		// out);
		// dfu.writeFile(decryptedKeyringKeyFile, out.toByteArray());
		// // encrypt key with user's public key (think: smartcard)
		// byte[] encKeyringKeyData = RSAEncryptUtil.encrypt(out.toByteArray(),
		// id.getPublickey(KeyType.ENCIPHERMENT));
		// // save encrypted key in a file
		// dfu.writeFile(encryptedKeyringKey, encKeyringKeyData);
		// // create new container
		// keyringContainer.setSize(Long.parseLong(CubeCommonProperties.getProperty(KEYRING_DEFAULT_SIZE_PROP)));
		// LOG.debug("Create new keyring's container [{}] [{} MB]",
		// keyringContainer.getContainerFile().getAbsolutePath(),
		// keyringContainer.getSize() / 1024 / 1024);
		// containerFactory.createContainer(keyringContainer, key);
		// }
		// // Open keyring container (at this point container and key should
		// // exists, key should be available 'decrypted')
		// try {
		// // open container
		// LOG.debug("Open keyring: container[{}]  key[{}]",
		// keyringContainer.getContainerFile().getAbsolutePath(),
		// keyringContainer.getMountpoint()
		// .getAbsolutePath());
		// containerFactory.mountContainer(keyringContainer, key);
		// } finally {
		// // shred & delete decrypted key
		// key.shred();
		// }
		// } catch (Exception e) {
		// throw new KeyringException("Failed to init keyring", e);
		// }
	}

	private final void decryptKey(File srcFile, SafeFile dstFile, IIdentityToken id) throws IOException, CubeException {
		// decrypt key
		// (@TODO if key was encrypted with an older key (not valid
		// anymore), we MUST re-encrypt it with the new one)
		// (@TODO provide a way to request the password if needed)
		dfu.writeFile(dstFile, RSAEncryptUtil.decrypt(dfu.readFile(srcFile), id.getPrivatekey(KeyType.ENCIPHERMENT)));
	}

	@Override
	public void close() throws KeyringException {
		LOG.debug("Close keyring");
		try {
			containerFactory.unmountContainer(keyringContainer);
		} catch (ContainerException e) {
			throw new KeyringException("Failed to close keyring", e);
		}
	}

	@Override
	public void createKey(String keyId) throws KeyringException {
		String pfid = KEYID_PREFIX + keyId;
		try {
			if (ds.dataExists(pfid, id)) {
				throw new KeyringException("keyfile already present in keyring at id [" + keyId + "]");
			} else {
				// create new random key (in memory)
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				KeyGenerator.generateKey(Integer.parseInt(CubeCommonProperties.getProperty(KEYRING_DEFAULT_KEY_BIT_SIZE_PROP)), baos);
				// store it
				ds.dataStore(pfid, baos.toByteArray(), id);
			}
		} catch (Exception e) {
			throw new KeyringException("Failed to create key [" + keyId + "]", e);
		}
	}

	@Override
	public EncryptionKey getKey(String keyId) throws KeyringException {
		String pfid = KEYID_PREFIX + keyId;
		try {
			if (ds.dataExists(pfid, id)) {
				// Retrieve key and store in 'clear text' form in a file in the
				// safe directory.
				File decFile = new File(safeDir, generatedRandomFilename());
				decFile.deleteOnExit();
				dfu.writeFile(decFile, ds.dataRetrieve(pfid, id));
				return new EncryptionKey(keyId, decFile);
			} else {
				throw new KeyringException("keyfile does not exist in keyring at id [" + keyId + "]");
			}
		} catch (Exception e) {
			throw new KeyringException("Failed to retrieve key [" + keyId + "]", e);
		}
	}

	@Override
	public File getFile(String uri) {
		File f = new File(keyringContainer.getMountpoint(), uri);
		LOG.debug("return file [{}]", f);
		return f;
	}

	@Override
	public void removeKey(String keyId) throws KeyringException {
		eraseData(keyId);
	}

	private String generatedRandomFilename() throws IOException {
		return Base64.encodeBytes(UuidGenerator.generate().getBytes(), Base64.URL_SAFE);
	}

	@Override
	public SafeFile retrieveDataAsFile(String id) throws KeyringException {
		try {
			byte[] data = retrieveData(id);
			SafeFile decFile = new SafeFile(safeDir, generatedRandomFilename());
			decFile.deleteOnExit();
			dfu.writeFile(decFile, data);
			if (!decFile.setReadOnly()) {
				LOG.error("Failed to set file as read-only");
			}
			return decFile;
		} catch (Exception e) {
			throw new KeyringException("failed to store data", e);
		}
	}

	@Override
	public void eraseData(String id) throws KeyringException {
		try {
			String encId = Base64.encodeBytes(("data--" + HashUtil.sha512UrlInBase64(id)).getBytes(), Base64.URL_SAFE);
			File kFile = new File(keyringContainer.getMountpoint(), encId + ".key");
			File dFile = new File(keyringContainer.getMountpoint(), encId + ".data");
			if (kFile.exists()) {
				dfu.shredFile(kFile);
			}
			if (dFile.exists()) {
				dfu.shredFile(dFile);
			}
		} catch (Exception e) {
			throw new KeyringException("failed to erase data", e);
		}
	}

	@Override
	public void deleteEntireKeyring() {
		if (!keyringContainer.getContainerFile().delete()) {
			LOG.warn("Failed to delete keyring.");
		}
		File encryptedKeyringKey = new File(//
				new File(CubeCommonProperties.getProperty(KEYRING_KEYS_DIR_PROP)), //
				id.getUuidHash() + KEYRING_KEY_FILEEXTENSION);
		if (!encryptedKeyringKey.delete()) {
			LOG.warn("Failed to delete keyring's encrypted key.");
		}
	}

	@Override
	public byte[] retrieveData(String dataId) throws KeyringException {
		return ds.dataRetrieve(dataId, id);
	}

	@Override
	public void storeData(byte[] data, String dataId) throws KeyringException {
		ds.dataStore(dataId, data, id);
	}

	@Override
	public void setId(IIdentityToken id) {
		this.id = id;
	}
}
