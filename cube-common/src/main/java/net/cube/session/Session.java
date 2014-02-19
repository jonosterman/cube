package net.cube.session;

import java.io.File;

import net.cube.common.CubeConfig;
import net.cube.filesafe.FileSafe;
import net.cube.token.IIdentityToken;

public class Session {
	private IIdentityToken token;
	private FileSafe safe;
	private File sessionDir;

	public Session(IIdentityToken token) {
		this.token = token;
	}

	public IIdentityToken getToken() {
		return token;
	}

	public File getSessionDir() {
		return sessionDir;
	}

	public FileSafe getSafe() {
		return safe;
	}
	
	public void begin() {
		// session directory
		sessionDir = new File(CubeConfig.getBaseDir(), "sessions/" + token.getUuidHash());
		sessionDir.mkdirs();
		// setup file safe (use to store sensitive data like keys)
		safe = new FileSafe();
		safe.setup(new File(sessionDir,"safe"), token);
		safe.open();
		// 
	}

	public void end() {
		safe.close();
	}
}
