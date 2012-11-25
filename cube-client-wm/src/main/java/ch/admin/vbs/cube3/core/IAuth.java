package ch.admin.vbs.cube3.core;

public interface IAuth {

	void addListener(AuthListener l);

	void removeListener(AuthListener l);

	interface AuthListener {
		void userAuthenticated();

		void userShutdownRequest();
	}
}
