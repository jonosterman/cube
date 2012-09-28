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
package ch.admin.vbs.cube.core.crypt;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;

public class CryptPasswordChanger {
	private static final Logger LOG = LoggerFactory
			.getLogger(CryptPasswordChanger.class);
	public static final String US_KBD_ALLOWED_REGEX = "^[-\\w+/.,\\\\';\\]\\[=`~!@#$%^&*()_+}{|\":?><]+$";
	private static Pattern valid = Pattern.compile(US_KBD_ALLOWED_REGEX);
	public boolean changePassword(final String oldPw, final String newPw) {
		String dev = CubeClientCoreProperties.getProperty("cryptsetup.dev");
		if (dev == null) {
			LOG.error("no device specified in configuration file");
			return false;
		}
		if (!valid.matcher(oldPw).matches() || !valid.matcher(newPw).matches()) {
			LOG.error("invalid characters in user submited passowrd");
			return false;
		}
		LOG.debug("Change password on device [{}]",dev);
		ScriptUtil script = new ScriptUtil();
		try {
			ShellUtil su = script.executeWithStdin(oldPw + "\n"+newPw + "\n"+newPw + "\n",  "sudo", "./dmcrypt-chgkey.pl", //
					"-d", dev//
			);			
			return su.getExitValue() == 0;
		} catch (Exception e) {
			LOG.error("Failed to update password",e);
		}
		return false;
	}
}
