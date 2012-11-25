package ch.admin.vbs.cube3.core.impl;

import java.util.ArrayList;

import ch.admin.vbs.cube3.core.ILogin;
import ch.admin.vbs.cube3.core.IToken;
import ch.admin.vbs.cube3.core.IToken.ITokenListener;
import ch.admin.vbs.cube3.core.IToken.TokenEvent;

public class Login implements ILogin, ITokenListener {
	private ArrayList<LoginListener> listeners = new ArrayList<ILogin.LoginListener>(2);

	public Login() {
	}

	@Override
	public void tokenEvent(TokenEvent e) {
		switch (e) {
		case INSERTED:
			break;
		case REMOVED:
			break;
		default:
			break;
		}
	}

	public void setup(IToken token) {
		token.addListener(this);
	}

	public void start() {
	}

	@Override
	public void addListener(LoginListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(LoginListener l) {
		listeners.remove(l);
	}
}
