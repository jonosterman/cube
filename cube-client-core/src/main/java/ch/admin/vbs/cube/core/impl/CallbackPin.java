package ch.admin.vbs.cube.core.impl;

import ch.admin.vbs.cube.core.AbstractUICallback;
import ch.admin.vbs.cube.core.IAuthModule;

public class CallbackPin extends AbstractUICallback {

	protected char[] password;
	protected final IAuthModule authModule;

	public CallbackPin(IAuthModule authModule) {
		this.authModule = authModule;
	}

	public void setPassword(char[] password ) {
		this.password = password;
	}
	
	@Override
	public void process() {
		authModule.setPassword(password);
	}

	@Override
	public void aborted() {
		authModule.abort();
	}
}
