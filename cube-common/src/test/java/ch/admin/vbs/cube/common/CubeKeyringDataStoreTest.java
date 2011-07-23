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

import java.io.ByteArrayOutputStream;

import org.junit.Assert;
import org.junit.Test;

import ch.admin.vbs.cube.common.keyring.impl.KeyGenerator;

/**
 * Test keyring creation and usage. Use CubeKeyring with DmCrypt encryption and
 * a test P12 file.
 */
public class CubeKeyringDataStoreTest extends AbstractCubeKeyringTest {
	@Test
	public void testFakeKeyring() throws Exception {
		initKeyring();
		// test data (random)
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		KeyGenerator.generateKey(80000, baos);
		byte[] data = baos.toByteArray();
		// store data
		keyring.storeData(data, "test-data");
		// retrieve data
		byte[] ret = keyring.retrieveData("test-data");
		// compare data
		Assert.assertArrayEquals(data, ret);
		// cleanup
		disposeKeyring();
	}

	public static void main(String[] args) throws Exception {
		CubeKeyringDataStoreTest t = new CubeKeyringDataStoreTest();
		t.testFakeKeyring();
	}
}
