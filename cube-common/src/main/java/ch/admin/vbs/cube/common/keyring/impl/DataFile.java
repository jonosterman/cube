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

package ch.admin.vbs.cube.common.keyring.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ShellUtil;

/**
 * Read/write data from/to file
 */
public class DataFile {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(DataFile.class);
	private Executor exec = Executors.newCachedThreadPool();

	/** read a keyfile and return it as a byte array. */
	public byte[] readFile(File file) throws IOException {
		byte[] ret = new byte[(int) file.length()];
		FileInputStream fis = new FileInputStream(file);
		fis.read(ret);
		fis.close();
		return ret;
	}

	/** Write a key (byte array) on the disk. */
	public void writeFile(File file, byte[] data) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(data);
		fos.close();
	}

	/** rewrite random data on a file and delete it. */
	public void shredFile(final File file) throws KeyringException {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if (file != null && file.exists()) {
						file.setWritable(true);
						ShellUtil su = new ShellUtil();
						su.run(null, ShellUtil.NO_TIMEOUT, "shred", "-u", file.getAbsolutePath());
					}
				} catch (Exception e) {
					LOG.error("failed to shred data", e);
				}
			}
		});
	}
}
