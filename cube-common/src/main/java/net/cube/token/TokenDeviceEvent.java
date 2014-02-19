package net.cube.token;

import net.cube.token.ITokenDevice.EventType;

public class TokenDeviceEvent {
	private final IIdentityToken token;
	private final EventType type;

	public TokenDeviceEvent(EventType type, IIdentityToken token) {
		this.type = type;
		this.token = token;
	}

	public IIdentityToken getToken() {
		return token;
	}

	public EventType getType() {
		return type;
	}
}