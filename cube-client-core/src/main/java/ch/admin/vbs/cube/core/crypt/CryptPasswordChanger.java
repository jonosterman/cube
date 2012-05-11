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
	private static Pattern valid = Pattern.compile("[-a-zA-Z0-9_+*%()&/\\?!\\[\\]\\{\\}\\.,:;@#\\|]+");
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
