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

package ch.admin.vbs.cube.common.keyring.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate random key (used by dmcrypt as keyfile)
 */
public class KeyGenerator {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(KeyGenerator.class);

	public static void generateKey(int bitSize, OutputStream os) throws IOException {
		Random rnd = new Random(System.currentTimeMillis());
		// it will round the bit size
		int x = 0;
		for (int i = 0; i < bitSize; i += 8) {
			os.write(rnd.nextInt());
			x++;
		}
		LOG.debug("generated a [{} bits] key.", x * 8);
		os.flush();
	}
}
