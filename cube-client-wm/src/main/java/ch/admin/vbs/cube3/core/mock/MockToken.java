package ch.admin.vbs.cube3.core.mock;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.IScreenManager;
import ch.admin.vbs.cube.client.wm.ui.dialog.ButtonLessDialog;
import ch.admin.vbs.cube.client.wm.ui.dialog.CubeMessageDialog;
import ch.admin.vbs.cube3.core.ILogin;
import ch.admin.vbs.cube3.core.IToken;
import ch.admin.vbs.cube3.core.IToken.ITokenListener;

public class MockToken implements IToken {
	private static final Logger LOG = LoggerFactory.getLogger(MockToken.class);
	private ArrayList<ITokenListener> listeners = new ArrayList<ITokenListener>(2);

	public void setup() {
	}

	@Override
	public void addListener(ITokenListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(ITokenListener l) {
		listeners.remove(l);
	}

	private void fireEvent(TokenEvent e) {
		for (ITokenListener l : listeners) {
			l.tokenEvent(e);
		}
	}

	public void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						Thread.sleep(5000);
						fireEvent(TokenEvent.INSERTED);
						Thread.sleep(5000);
						fireEvent(TokenEvent.REMOVED);
					}
					//
					// LOG.debug("Open MockToken dialog.");
					// ButtonLessDialog msg = new
					// ButtonLessDialog(screenManager.getDefaultScreen().getMessageFrame(),
					// "Please Insert token..." );
					// msg.displayWizardLater();
					// Thread.sleep(3000);
					// msg.hideWizard();
					// msg.dispose();
					//
					// login.showLogin();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}