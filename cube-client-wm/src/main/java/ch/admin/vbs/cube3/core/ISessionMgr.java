package ch.admin.vbs.cube3.core;

import ch.admin.vbs.cube3.core.obj.ISession;

public interface ISessionMgr {
	
	
	void addListener(ISessionsChangeListener l);
	void removeListener(ISessionsChangeListener l);
	
	interface ISessionsChangeListener {
		void activeSessionChanged(ISession session);
	}
}
