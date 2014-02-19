package net.cube.token;

public interface ITokenDevice {
	enum EventType {
		INSERTED, REMOVED
	}

	void addListener(ITokenDeviceListener l);

	void removeListener(ITokenDeviceListener l);
}
