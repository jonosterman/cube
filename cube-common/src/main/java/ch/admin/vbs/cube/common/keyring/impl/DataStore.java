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

package ch.admin.vbs.cube.common.keyring.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import ch.admin.vbs.cube.common.crypto.AESEncrypter;
import ch.admin.vbs.cube.common.crypto.Base64;
import ch.admin.vbs.cube.common.crypto.HashUtil;
import ch.admin.vbs.cube.common.crypto.RSAEncryptUtil;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.IIdentityToken.KeyType;

public class DataStore {
	private DataFile dfu = new DataFile();
	private File store;

	public DataStore() {
	}

	public void setStoreDirectory(File store) {
		this.store = store;
	}

	public void dataStore(String dataId, byte[] data, IIdentityToken id) throws KeyringException {
		String encId = formatId(dataId);
		try {
			SecretKey skey = AESEncrypter.generateKey(128);
			// encrypt data with symmetric key and place it in a file also in
			// the keyring
			AESEncrypter c = new AESEncrypter(skey);
			FileOutputStream fos = new FileOutputStream(new File(store, encId + ".data"));
			c.encrypt(new ByteArrayInputStream(data), fos);
			fos.close();
			// write encrypted symmetric key in a file in keyring
			byte[] encSkey = RSAEncryptUtil.encrypt(skey.getEncoded(), id.getPublickey(KeyType.ENCIPHERMENT));
			dfu.writeFile(new File(store, encId + ".key"), encSkey);
		} catch (Exception e) {
			throw new KeyringException("failed to store data", e);
		}
	}

	public byte[] dataRetrieve(String dataId, IIdentityToken id) throws KeyringException {
		String encId = formatId(dataId);
		try {
			File kFile = new File(store, encId + ".key");
			if (!kFile.exists()) {
				return null;
			}
			byte[] encskey = dfu.readFile(kFile);
			byte[] skey = RSAEncryptUtil.decrypt(encskey, id.getPrivatekey(KeyType.ENCIPHERMENT));
			// decrypt data
			SecretKeySpec sp = new SecretKeySpec(skey, "AES");
			AESEncrypter c = new AESEncrypter(sp);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			FileInputStream fi = new FileInputStream(new File(store, encId + ".data"));
			c.decrypt(fi, baos);
			fi.close();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new KeyringException("failed to retrieve data", e);
		}
	}

	/**
	 * Check that data exists and is readable
	 * 
	 * @param dataId
	 * @param id
	 * @return
	 * @throws KeyringException
	 */
	public boolean dataExists(String dataId, IIdentityToken id) throws KeyringException {
		String encId = formatId(dataId);
		try {
			File kFile = new File(store, encId + ".key");
			if (!kFile.exists()) {
				return false;
			}
			byte[] encskey = dfu.readFile(kFile);
			byte[] skey = RSAEncryptUtil.decrypt(encskey, id.getPrivatekey(KeyType.ENCIPHERMENT));
			// decrypt data
			SecretKeySpec sp = new SecretKeySpec(skey, "AES");
			AESEncrypter c = new AESEncrypter(sp);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			FileInputStream fi = new FileInputStream(new File(store, encId + ".data"));
			c.decrypt(fi, baos);
			fi.close();
			return true;
		} catch (Exception e) {
			throw new KeyringException("failed to retrieve data", e);
		}
	}

	/**
	 * Since user submitted ID are free text strings, we have to convert them in
	 * another string which can be used as a filename to store them (filesystem
	 * does not support any character as part of the filename).
	 * 
	 * @throws KeyringException
	 */
	private String formatId(String id) throws KeyringException {
		try {
			return Base64.encodeBytes(("Cube--" + HashUtil.sha512UrlInBase64(id)).getBytes(), Base64.URL_SAFE);
		} catch (IOException e) {
			throw new KeyringException("Failed to encode id", e);
		}
	}
}
