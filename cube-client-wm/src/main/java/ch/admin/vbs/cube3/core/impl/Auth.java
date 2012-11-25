package ch.admin.vbs.cube3.core.impl;

import java.util.ArrayList;

import ch.admin.vbs.cube3.core.IAuth;
import ch.admin.vbs.cube3.core.IToken;
import ch.admin.vbs.cube3.core.IToken.ITokenListener;
import ch.admin.vbs.cube3.core.IToken.TokenEvent;

public class Auth implements IAuth, ITokenListener {
	private ArrayList<AuthListener> listeners = new ArrayList<AuthListener>(2);

	public Auth(IToken token) {
		token.addListener(this);
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

	public void setup() {
	}

	public void start() {
	}

	@Override
	public void addListener(AuthListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(AuthListener l) {
		listeners.remove(l);
	}
}
