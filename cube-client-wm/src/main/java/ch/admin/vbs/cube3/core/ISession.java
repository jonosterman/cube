package ch.admin.vbs.cube3.core;


public interface ISession {
	
	void addListener(ISessionChangeListener l);
	void removeListener(ISessionChangeListener l);

	interface ISessionChangeListener {
		void vmListChanged();
	}
}
