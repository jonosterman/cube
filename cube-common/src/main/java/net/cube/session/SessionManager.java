package net.cube.session;

import java.util.HashMap;

import net.cube.token.ITokenDevice;
import net.cube.token.ITokenDeviceListener;
import net.cube.token.TokenDeviceEvent;

public class SessionManager implements ITokenDeviceListener {
	private HashMap<String, Session> cache = new HashMap<String, Session>();

	public void setup(ITokenDevice tokenDevice) {
		tokenDevice.addListener(this);
	}

	@Override
	public void handle(TokenDeviceEvent event) {
		Session session = null;
		switch (event.getType()) {
		case INSERTED:
			session = new Session(event.getToken());
			cache.put(event.getToken().getUuid(), session);
			session.begin();
			break;
		case REMOVED:
		default:
			session = cache.remove(event.getToken().getUuid());
			session.end();
			break;
		}
	}
}
