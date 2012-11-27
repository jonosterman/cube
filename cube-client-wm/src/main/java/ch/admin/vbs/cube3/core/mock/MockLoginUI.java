package ch.admin.vbs.cube3.core.mock;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.impl.TokenEvent;
import ch.admin.vbs.cube3.core.ILoginUI;

public class MockLoginUI implements ILoginUI, ITokenListener {
	private static final Logger LOG = LoggerFactory.getLogger(MockLoginUI.class);
	private ArrayList<ILoginUIListener> listeners = new ArrayList<ILoginUI.ILoginUIListener>();
	private final char[] passwd;

	public MockLoginUI(String passwd) {
		this.passwd = passwd.toCharArray();
	}

	public void start() {
	}

	public void setup(ITokenDevice td) {
		td.addListener(this);
	}

	@Override
	public void addListener(ILoginUIListener l) {
		listeners.add(l);
	}

	@Override
	public void notifyTokenEvent(TokenEvent event) {
		switch (event.getType()) {
		case TOKEN_INSERTED:
			LOG.debug("Provide dummy password.");
			for (ILoginUIListener l : listeners) {
				l.setPassord(passwd);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void removeListener(ILoginUIListener l) {
		listeners.remove(l);
	}
}
