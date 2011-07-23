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
import java.security.KeyStoreException;

import ch.admin.vbs.cube.common.container.impl.DmcryptContainerFactory;
import ch.admin.vbs.cube.common.keyring.impl.CubeKeyring;
import ch.admin.vbs.cube.common.keyring.impl.IdentityToken;
import ch.admin.vbs.cube.common.keyring.impl.KeyringException;

public abstract class AbstractCubeKeyringTest {
	protected static final String testKeystorePassword = "111222";
	protected static final String testKeystoreFile = "/cube-01_pwd-is-111222.p12";
	protected Builder builder;
	protected KeyStore keystore;
	protected IdentityToken id;
	protected CubeKeyring keyring;

	protected void disposeKeyring() throws KeyringException {
		System.out.println("Close keyring..");
		keyring.close();
		// cleanup
		System.out.println("Remove test keyring..");
		keyring.deleteEntireKeyring();
	}

	protected void initKeyring() throws KeyStoreException, KeyringException {
		DmcryptContainerFactory.cleanup();
		// Open KeyStore
		System.out.println("Open keystore..");
		File p12File = new File(getClass().getResource(testKeystoreFile).getFile());
		builder = KeyStore.Builder.newInstance("PKCS12", null, p12File, new KeyStore.PasswordProtection(testKeystorePassword.toCharArray()));
		keystore = builder.getKeyStore();
		// Setup Identity
		System.out.println("Create Identity..");
		id = new IdentityToken(keystore, builder, testKeystorePassword.toCharArray());
		// Open Keyring
		System.out.println("Open keyring..");
		keyring = new CubeKeyring();
		File safeDir = new File(System.getProperty("java.io.tmpdir"));
		keyring.open(id, safeDir);
	}
}
