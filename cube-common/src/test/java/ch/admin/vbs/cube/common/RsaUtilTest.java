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

package ch.admin.vbs.cube.common;

import java.io.File;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import ch.admin.vbs.cube.common.crypto.RSAEncryptUtil;

/**
 * Test RSA encrpytion util using a P12 file private/public key.
 * 
 */
public class RsaUtilTest {
	private static final String testKeystorePassword = "111222";
	private static final String testKeystoreFile = "/cube-01_pwd-is-111222.p12";
	private Random rnd = new Random(System.currentTimeMillis());
	@Test
	public void testRsaUtil() throws Exception {
		// Read test P12
		File p12File = new File(getClass().getResource(testKeystoreFile).getFile());
		Builder builder = KeyStore.Builder.newInstance("PKCS12", null, p12File, new KeyStore.PasswordProtection(testKeystorePassword.toCharArray()));
		KeyStore keystore = builder.getKeyStore();
		String alias = keystore.aliases().nextElement();
		System.out.printf("Use certificate [%s]\n", alias);
		PublicKey pubKey = keystore.getCertificate(alias).getPublicKey();
		PrivateKey privKey = (PrivateKey) keystore.getKey(alias, testKeystorePassword.toCharArray());
		System.out.println("Public ------------");
		System.out.println(pubKey);
		System.out.println("Private -----------");
		System.out.println(privKey);
		System.out.println("-------------------");
		// test P12 use 1024 bits keys
		System.setProperty(RSAEncryptUtil.KEYLENGTH_PROPERTY, "2048");
		System.out.println("Use algorithm key size [" + System.getProperty(RSAEncryptUtil.KEYLENGTH_PROPERTY) + "]");
		// rsa util
		byte[] original = new byte[4000];
		rnd.nextBytes(original);
		// encrypt
		byte[] encrypted = RSAEncryptUtil.encrypt(original, pubKey);
		// decrypt
		byte[] decrypted = RSAEncryptUtil.decrypt(encrypted, privKey);
		Assert.assertArrayEquals("decrypted text does not match original", original, decrypted);
		System.out.println("done");
	}

	public static void main(String[] args) throws Exception {
		RsaUtilTest t = new RsaUtilTest();
		t.testRsaUtil();
	}
}
