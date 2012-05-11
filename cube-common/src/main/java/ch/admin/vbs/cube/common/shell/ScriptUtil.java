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

package ch.admin.vbs.cube.common.shell;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeCommonProperties;

/**
 * Helper class used to execute cube scripts
 */
public class ScriptUtil {
	private static final String CUBE_SCRIPT_DIR = "cube.scripts.dir";
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ScriptUtil.class);
	private File baseDir;

	public ScriptUtil() {
		String baseDirname = CubeCommonProperties.getProperty(CUBE_SCRIPT_DIR);
		LOG.debug("Init script util with [" + baseDirname + "]");
		baseDir = new File(baseDirname);
		if (!baseDir.exists()) {
			LOG.error("script directory [{}] does not exists.", baseDirname);
			return;
		}
	}

	/**
	 * Execute command in script directory
	 */
	public ShellUtil execute(String... commandWithArgs) throws ShellUtilException {
		ShellUtil shell = new ShellUtil();
		shell.run(baseDir, ShellUtil.NO_TIMEOUT, commandWithArgs);
		return shell;
	}
	/**
	 * Execute command in script directory
	 */
	public ShellUtil executeWithStdin(String stdin, String... commandWithArgs) throws ShellUtilException {
		ShellUtil shell = new ShellUtil();
		shell.setStdIn(stdin);
		shell.run(baseDir, ShellUtil.NO_TIMEOUT, commandWithArgs);
		return shell;
	}
}
