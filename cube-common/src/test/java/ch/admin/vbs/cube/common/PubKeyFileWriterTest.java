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
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import ch.admin.vbs.cube.common.crypto.RSAEncryptUtil;
import ch.admin.vbs.cube.tmp.KeyASCIIOutput;

/**
 * Test RSA encrpytion/decryption util 'RSAEncryptUtil' using a P12 file
 * private/public keys.
 */
public class PubKeyFileWriterTest {
	private static final String KEY_LENGTH = "2048";
	private static final String testKeystorePassword = "111222";
	private static final String testKeystoreFile = "/cube-01_pwd-is-111222.p12";
	private Random rnd = new Random(System.currentTimeMillis());

	@Test
	public void testPubKeyWrite() throws Exception {
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
		//
		KeyASCIIOutput out = new KeyASCIIOutput();
		FileOutputStream fos = new FileOutputStream("/tmp/jojo.pub");
		out.write(fos, pubKey);
		fos.close();
		System.out.println("done");
	}

	public static void main(String[] args) throws Exception {
		PubKeyFileWriterTest t = new PubKeyFileWriterTest();
		t.testPubKeyWrite();
	}
}
