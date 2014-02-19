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

package test.old;

import java.io.File;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Random;

import net.cube.common.crypto.RSAEncryptUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test RSA encrpytion/decryption util 'RSAEncryptUtil' using a P12 file
 * private/public keys.
 */
public class RsaUtilTest {
	private static final String KEY_LENGTH = "2048";
	private static final String testKeystorePassword = "111222";
	private static final String testKeystoreFile = "/cube-01_pwd-is-111222.p12";
	private Random rnd = new Random(System.currentTimeMillis());

	@Test
	public void testRsaUtil() throws Exception {
		// Read test P12
		File p12File = new File(getClass().getResource(testKeystoreFile)
				.getFile());
		Builder builder = KeyStore.Builder.newInstance(
				"PKCS12",
				null,
				p12File,
				new KeyStore.PasswordProtection(testKeystorePassword
						.toCharArray()));
		KeyStore keystore = builder.getKeyStore();
		// pick first alias in keystore and retrieve its private and public key
		String alias = keystore.aliases().nextElement();
		PublicKey pubKey = keystore.getCertificate(alias).getPublicKey();
		PrivateKey privKey = (PrivateKey) keystore.getKey(alias,
				testKeystorePassword.toCharArray());
		// set the right key length
		System.setProperty(RSAEncryptUtil.KEYLENGTH_PROPERTY, KEY_LENGTH);
		// create message to encrypt (random data)
		byte[] original = new byte[4000];
		rnd.nextBytes(original);
		// encrypt message
		byte[] encrypted = RSAEncryptUtil.encrypt(original, pubKey);
		// decrypt message
		byte[] decrypted = RSAEncryptUtil.decrypt(encrypted, privKey);
		// assert both original and encrypted message are the same
		Assert.assertArrayEquals("decrypted text does not match original",
				original, decrypted);
		System.out.println("Message of size [" + original.length
				+ "] encrypted and decypted by [" + alias + "] with a key of ["
				+ KEY_LENGTH + "] bits ..... OK");
	}

	public static void main(String[] args) throws Exception {
		RsaUtilTest t = new RsaUtilTest();
		t.testRsaUtil();
	}
}
