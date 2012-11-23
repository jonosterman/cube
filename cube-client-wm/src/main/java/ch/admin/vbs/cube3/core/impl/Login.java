package ch.admin.vbs.cube3.core.impl;

import java.util.ArrayList;

import ch.admin.vbs.cube.client.wm.ui.dialog.PasswordDialog;
import ch.admin.vbs.cube3.core.ILogin;

public class Login implements ILogin {
	private ArrayList<LoginListener> listeners = new ArrayList<ILogin.LoginListener>(2);

	public Login() {
	}

	@Override
	public void showLogin() {
		PasswordDialog dial = new PasswordDialog(null);
		dial.displayWizard();
	}

	public void setup() {
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
