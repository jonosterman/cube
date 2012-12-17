package ch.admin.vbs.cube3.core;


public interface IWSClient {
	public enum Event {
		WS_CONNECTED, WS_CONNECTION_ERROR
	}

	void addListener(IWSClientListener l);

	void removeListener(IWSClientListener l);

	interface IWSClientListener {
		void processEvent(Event e);
	}
}
