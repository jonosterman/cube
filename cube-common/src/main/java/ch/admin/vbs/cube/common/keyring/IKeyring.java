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

package ch.admin.vbs.cube.common.keyring;

import java.io.File;

import ch.admin.vbs.cube.common.keyring.impl.KeyringException;

/**
 * The keyring is responsible to hold encrypted material on the disk. It holds
 * keys and user data.
 * 
 * The keyring itself should be encrypted, and since it is open at the beginning
 * of a user session, all data stored in it should be individually encrypted.
 * 
 * Keyring need a safe directory to temporary store decrypted file. This
 * directory should be located on an encrypted partition.
 * 
 */
public interface IKeyring {
	/**
	 * Open keyring. Create it if it does not already exist.
	 * 
	 * @param userId
	 *            user ID. Have to be unique (could be used to define keyring
	 *            file/directory name)
	 * @param pubKey
	 *            User public key used to encrypt keyring and data in it.
	 * @param privKey
	 *            User private key used to decrypt data.
	 * @param safeDir
	 *            A safe place where sensitive data could be temporary saved
	 *            un-encrypted. (key file to be used by dmcrypt, openvpn, etc)
	 */
	void open(IIdentityToken id, File safeDir) throws KeyringException;

	/**
	 * Close keyring.
	 */
	void close() throws KeyringException;

	/**
	 * Delete entire keyring (with all keys & data)
	 */
	void deleteEntireKeyring();

	/**
	 * Key Management: create a file filled with random data (keyfile) and store
	 * it in keyring.
	 */
	void createKey(String id) throws KeyringException;

	/**
	 * Key Management: retrieve an existing keyfile from keyring
	 */
	EncryptionKey getKey(String id) throws KeyringException;

	/**
	 * Key Management: delete an keyfile in keyring
	 */
	void removeKey(String id) throws KeyringException;

	/** Data management: store data, encrypted, in keyring. */
	void storeData(byte[] data, String id) throws KeyringException;

	/** Data management: retrieve data, decrypted, from keyring. */
	byte[] retrieveData(String id) throws KeyringException;

	/**
	 * Data management: retrieve data, as file in an encrypted partition,
	 * decrypted, from keyring.
	 */
	SafeFile retrieveDataAsFile(String id) throws KeyringException;

	/** Data management: erase data in keyring. */
	void eraseData(String id) throws KeyringException;

	/** @retrun an unencrypted file reference in keyring (keyring is encrypted) */
	File getFile(String filename) throws KeyringException;

	void setId(IIdentityToken id);
}
