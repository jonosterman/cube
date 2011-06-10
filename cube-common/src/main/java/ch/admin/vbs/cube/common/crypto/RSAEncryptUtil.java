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

package ch.admin.vbs.cube.common.crypto;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeException;

/**
 * Utility class that helps encrypt and decrypt strings using RSA algorithm.<br>
 * This class has been inspired by the blog entry
 * http://www.aviransplace.com/2004/10/12/using-rsa-encryption-with-java/ which
 * explains in a very concise way how RSA works.
 * 
 * 27.4.2010: add a loop in order to encode larger data blocks. RSA
 * should not be used to encrypt large data (very slow) but 117 bytes (936 bits)
 * is way not enough (for example to encrypt a 4096 bits rsa key). <br>
 * 
 */
public final class RSAEncryptUtil {
	// used to configure key length via system properties
	public static final String KEYLENGTH_PROPERTY = "RSAEncryptUtil.keyLength";
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(RSAEncryptUtil.class);

	/**
	 * Prevents instantiation.
	 */
	private RSAEncryptUtil() {
	}

	/**
	 * Encrypt a text using public key.
	 * 
	 * @param text
	 *            The original unencrypted text.
	 * @param key
	 *            The public key.
	 * @return Encrypted text as a byte-array.
	 * @throws CubeException
	 *             if a problem arises during encryption.
	 */
	public static byte[] encrypt(byte[] text, PublicKey key) throws CubeException {
		try {
			// the maximal size of the block we are able to encrypt/decrypt at
			// once with the key, depends of the size of this key. Therefore we
			// left the possibility to configure this size through JVM
			// properties.
			int keyLengthInBit = 2048; // default
			String systemDefinedKeyLength = System.getProperty(KEYLENGTH_PROPERTY);
			if (systemDefinedKeyLength != null) {
				keyLengthInBit = Integer.parseInt(systemDefinedKeyLength);
			}
			LOG.debug("Use keylength [{} bits]", keyLengthInBit);
			// the text is proceeded in block of N bytes. N depends on the RSA
			// key size.
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			int encBlockSize = keyLengthInBit / 8 - 11;
			for (int i = 0; i < text.length; i += encBlockSize) {
				byte[] cipherText = cipher.doFinal(text, i, Math.min(encBlockSize, text.length - i));
				baos.write(cipherText);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			throw new CubeException("Problem during encryption.", e);
		}
	}

	/**
	 * Decrypt text using private key.
	 * 
	 * @param encryptedText
	 *            The encrypted text.
	 * @param key
	 *            The private key.
	 * @return The unencrypted text.
	 * @throws CubeException
	 *             if a problem arises during decryption.
	 */
	public static byte[] decrypt(byte[] encryptedText, PrivateKey key) throws CubeException {
		try {
			// the maximal size of the block we are able to encrypt/decrypt at
			// once with the key, depends of the size of this key. Therefore we
			// left the possibility to configure this size through JVM
			// properties.
			int keyLengthInBit = 2048; // default
			String systemDefinedKeyLength = System.getProperty(KEYLENGTH_PROPERTY);
			if (systemDefinedKeyLength != null) {
				keyLengthInBit = Integer.parseInt(systemDefinedKeyLength);
			}
			LOG.debug("Use keylength [{} bits]", keyLengthInBit);
			// the text is proceeded in block of N bytes. N depends on the RSA
			// key size.
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			int decBlockSize = keyLengthInBit / 8;
			for (int i = 0; i < encryptedText.length; i += decBlockSize) {
				byte[] cipherText = cipher.doFinal(encryptedText, i, Math.min(decBlockSize, encryptedText.length - i));
				baos.write(cipherText);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			throw new CubeException("Problem during decryption.", e);
		}
	}

	public static KeyPair generateKeyPair(int sizeInBits) throws CubeException {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(sizeInBits);
			KeyPair key = keyGen.generateKeyPair();
			return key;
		} catch (Exception e) {
			throw new CubeException("Problem generating key pair.", e);
		}
	}
}
