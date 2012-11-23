package ch.admin.vbs.cube3.core.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.ui.dialog.CubeMessageDialog;
import ch.admin.vbs.cube3.core.ILogin;
import ch.admin.vbs.cube3.core.IToken;

public class MockToken implements IToken {
	private static final Logger LOG = LoggerFactory.getLogger(MockToken.class);
	private ILogin login;

	public void setup(ILogin login) {
		this.login = login;
	}

	public void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//
					LOG.debug("Open dialog.");
					CubeMessageDialog msg = new CubeMessageDialog(null, "Please Insert token...");
					msg.displayWizard();
					Thread.sleep(1000);
					msg.hideWizard();
					msg.dispose();
					//
					login.showLogin();					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}