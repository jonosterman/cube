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

package ch.admin.vbs.cube.common;

import java.io.File;
import java.security.KeyStore;
import java.security.KeyStore.Builder;

import org.junit.Test;

import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.common.keyring.impl.CubeKeyring;
import ch.admin.vbs.cube.common.keyring.impl.IdentityToken;

/**
 * Test keyring creation and usage. Use CubeKeyring with DmCrypt encryption and
 * a test P12 file.
 */
public class CubeKeyringDoubleMountTest {
	private static final String testKeystorePassword = "111222";
	private static final String testKeystoreFile = "/cube-01_pwd-is-111222.p12";

	@Test
	public void testCubeKeyring() throws Exception {
		// we need a public and a private key to test CubeKeyring
		File p12File = new File(getClass().getResource(testKeystoreFile).getFile());
		Builder builder = KeyStore.Builder.newInstance("PKCS12", null, p12File, new KeyStore.PasswordProtection(testKeystorePassword.toCharArray()));
		KeyStore keystore = builder.getKeyStore();
		// Open keyring
		IKeyring keyring = new CubeKeyring();
		File safeDir = new File(System.getProperty("java.io.tmpdir"));
		System.out.println("Open keyring..");
		IIdentityToken id = new IdentityToken(keystore, builder, testKeystorePassword.toCharArray());
		keyring.open(id, safeDir);
		System.out.println("Open keyring again!..");
		keyring.open(id, safeDir);
		System.out.println("Close keyring..");
		keyring.close();
		// cleanup
		keyring.deleteEntireKeyring();
	}

	public static void main(String[] args) throws Exception {
		CubeKeyringDoubleMountTest t = new CubeKeyringDoubleMountTest();
		t.testCubeKeyring();
	}
}
