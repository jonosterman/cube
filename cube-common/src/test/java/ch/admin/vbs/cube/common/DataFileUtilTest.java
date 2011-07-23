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
import java.util.Random;

import org.junit.Test;

import ch.admin.vbs.cube.common.keyring.impl.DataFile;

public class DataFileUtilTest {
	@Test
	public void testReadAndWriteAndShred() throws Exception {
		try {
			DataFile dfu = new DataFile();
			// generate
			File file1 = File.createTempFile("abcdef", ".dat");
			byte[] data1 = generateRandomData(1024);
			File file2 = File.createTempFile("abcdef", ".dat");
			byte[] data2 = generateRandomData(1024);
			// write
			dfu.writeFile(file1, data1);
			dfu.writeFile(file2, data2);
			// re-read
			byte[] data1b = dfu.readFile(file1);
			byte[] data2b = dfu.readFile(file2);
			// compare
			compare(data1, data1b);
			compare(data2, data2b);
		} catch (Exception e) {
			e.printStackTrace();
			throw (e);
		}
	}

	private void compare(byte[] a, byte[] b) {
		for (int l = 0; l < Math.max(a.length, b.length); l++) {
			if (a[l] != b[l])
				throw new RuntimeException("Invalid value at [" + l + "]");
		}
	}

	private byte[] generateRandomData(int l) {
		Random r = new Random();
		byte[] buffer = new byte[l];
		r.nextBytes(buffer);
		return buffer;
	}
}
