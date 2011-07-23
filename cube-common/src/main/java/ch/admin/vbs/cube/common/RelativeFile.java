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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Relative file is used when we have to keep a reference to the base folder of
 * a file/directory. It is useful to display short relative path to user when
 * dealing always with absolute path in the background.
 */
public class RelativeFile {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(RelativeFile.class);
	private final File file;
	private final File basedir;

	public RelativeFile(File file, File basedir) {
		this.file = file;
		this.basedir = basedir;
	}

	public File getFile() {
		return file;
	}

	public File getBasedir() {
		return basedir;
	}

	public String getRelativeFilename() {
		String f = file.getAbsolutePath();
		String b = basedir.getAbsolutePath();
		if (f.startsWith(b)) {
			return f.substring(b.length());
		} else {
			LOG.warn("Bad relative path: file[{}] basedir[{}]", f, b);
			return f;
		}
	}
}
