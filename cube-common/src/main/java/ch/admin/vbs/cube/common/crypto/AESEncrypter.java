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

package ch.admin.vbs.cube.common.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class symmetrically encrypts and decrypts files using AES and has been
 * inspired by
 * http://www.experts-exchange.com/Programming/Languages/Java/Q_23044531.html
 * 
 */
public class AESEncrypter {
	private static final int BUFFER_SIZE = 1024 * 4;
	private static final Logger LOG = LoggerFactory.getLogger(AESEncrypter.class);
	private Cipher ecipher;
	private Cipher dcipher;
	// Buffer used to transport the bytes from one stream to another
	private byte[] buf = new byte[BUFFER_SIZE];

	/**
	 * Creates a {@link AESEncrypter} instance initialized with the passed
	 * symmetric {@link SecretKey}.
	 * 
	 * @param key
	 *            the {@link SecretKey} this instance uses for encryption and
	 *            decryption.
	 */
	public AESEncrypter(SecretKey key) {
		// Create an 8-byte initialization vector
		byte[] iv = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };
		AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
		try {
			ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			dcipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			// CBC requires an initialization vector
			//iv = ecipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
			ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
			dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
		} catch (Exception e) {
			LOG.error("Error initializing ciphers.", e);
		}
	}

	/**
	 * Encrypts a file.
	 * 
	 * @param in
	 *            stream reading the file to encrypt.
	 * @param out
	 *            stream writing the encrypted file.
	 */
	public void encrypt(InputStream in, OutputStream out) {
		try {
			// Bytes written to out will be encrypted
			out = new CipherOutputStream(out, ecipher);
			// Read in the cleartext bytes and write to out to encrypt
			int numRead = 0;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			out.close();
		} catch (java.io.IOException e) {
			LOG.error("Error encrypting file.", e);
		}
	}

	/**
	 * Decrypts a file.
	 * 
	 * @param in
	 *            stream reading the file to decrypt.
	 * @param out
	 *            stream writing the decrypted file.
	 */
	public void decrypt(InputStream in, OutputStream out) {
		try {
			// Bytes read from in will be decrypted
			in = new CipherInputStream(in, dcipher);
			// Read in the decrypted bytes and write the cleartext to out
			int numRead = 0;
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			out.close();
		} catch (java.io.IOException e) {
			LOG.error("Error decrypting file.", e);
		}
	}

	/**
	 * Generates a key which may be used to in the constructor of this class.
	 * 
	 * @param keyLength
	 *            length of the generated key in bits.
	 * @return the generated key.
	 */
	public static SecretKey generateKey(int keyLength) {
		try {
			KeyGenerator kgen;
			kgen = KeyGenerator.getInstance("AES");
			kgen.init(keyLength);
			SecretKey key = kgen.generateKey();
			return key;
		} catch (NoSuchAlgorithmException e) {
			LOG.error("Error generating key.", e);
			return null;
		}
	}

	/**
	 * Short demo for this class.
	 * 
	 * @param args
	 *            not used.
	 */
	public static void main(String[] args) {
		// This is the string we are going to encrypt and decrypt.
		String decrypted = "I'm decrypted!";
		LOG.info(decrypted);
		// Encrypt the string
		// You have to install the "unlimited strength" JCE Policy to be able to
		// use a 256 bits key.
		AESEncrypter aesEncrypter = new AESEncrypter(generateKey(128));
		ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();
		aesEncrypter.encrypt(new ByteArrayInputStream(decrypted.getBytes()), encryptedOutput);
		LOG.info(new String(encryptedOutput.toByteArray()));
		// Decrypt the string
		ByteArrayOutputStream decryptedOutput = new ByteArrayOutputStream();
		aesEncrypter.decrypt(new ByteArrayInputStream(encryptedOutput.toByteArray()), decryptedOutput);
		LOG.info(new String(decryptedOutput.toByteArray()));
	}
}
