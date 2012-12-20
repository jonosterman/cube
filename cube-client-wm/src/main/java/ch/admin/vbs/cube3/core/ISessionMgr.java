package ch.admin.vbs.cube3.core;


public interface ISessionMgr {
	
	
	void addListener(ISessionsChangeListener l);
	void removeListener(ISessionsChangeListener l);
	
	interface ISessionsChangeListener {
		void activeSessionChanged(ISession session);
	}
}
