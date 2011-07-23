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

import org.junit.Assert;
import org.junit.Test;

import ch.admin.vbs.cube.common.keyring.EncryptionKey;

/**
 * Test keyring creation and usage. Use CubeKeyring with DmCrypt encryption and
 * a test P12 file. Keyring and its key are deleted after the test
 */
public class CubeKeyringTest extends AbstractCubeKeyringTest {
	@Test
	public void testCubeKeyring() throws Exception {
		initKeyring();
		// test -----
		System.out.println("Create test key..");
		keyring.removeKey("test");
		keyring.createKey("test");
		// test -----
		System.out.println("Retrieve test key..");
		EncryptionKey key = keyring.getKey("test");
		System.out.println("Check ID..");
		Assert.assertEquals("Key ID should be 'test'", "test", key.getId());
		System.out.println("decrypted key file [" + key.getFile().getAbsolutePath() + "]");
		Assert.assertTrue("key file should exists", key.getFile().exists());
		System.out.println("Remove Key..");
		// end of test -----
		keyring.removeKey("test");
		// cleanup
		disposeKeyring();
	}

	public static void main(String[] args) throws Exception {
		CubeKeyringTest t = new CubeKeyringTest();
		t.testCubeKeyring();
	}
}
