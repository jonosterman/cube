package ch.admin.vbs.cube3.core.mock;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.impl.TokenEvent;
import ch.admin.vbs.cube.core.impl.TokenEvent.EventType;

public class MockTokenDevice implements ITokenDevice, Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(MockTokenDevice.class);
	private ArrayList<ITokenListener> listeners = new ArrayList<ITokenListener>();
	private final long logInLogOutDelay;

	public MockTokenDevice(long logInLogOutDelay) {
		this.logInLogOutDelay = logInLogOutDelay;
	}
	
	public void start() {
		new Thread(this).start();
	}

	@Override
	public void run() {
		try {
			Thread.sleep(2000);
			fireStateChanged(true);
			Thread.sleep(logInLogOutDelay);
			for (int i=0;i<10;i++) {
				fireStateChanged(false);				
				Thread.sleep(logInLogOutDelay);
				fireStateChanged(true);				
				Thread.sleep(logInLogOutDelay);
			}
			fireStateChanged(false);				
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void fireStateChanged(boolean newState) {
		TokenEvent event = new TokenEvent(newState ? EventType.TOKEN_INSERTED : EventType.TOKEN_REMOVED);
		LOG.debug("Generated event [{}]",event.getType());
		for (ITokenListener l : listeners) {
			l.notifyTokenEvent(event);
		}
	}

	@Override
	public void addListener(ITokenListener l) {
		listeners.add(l);
	}

	@Override
	public boolean isTokenReady() {
		return true;
	}
	
	public void setup() {}
	
}
