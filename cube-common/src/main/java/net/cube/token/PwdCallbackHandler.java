package net.cube.token;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class PwdCallbackHandler implements CallbackHandler {
	private char[] pwd;

	public PwdCallbackHandler(char[] pwd) {
		this.pwd = pwd;
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (Callback cb : callbacks) {
			if (cb instanceof PasswordCallback) {
				((PasswordCallback) cb).setPassword(pwd);
			}
		}
	}
};