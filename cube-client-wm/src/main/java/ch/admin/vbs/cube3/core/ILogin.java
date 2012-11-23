package ch.admin.vbs.cube3.core;

public interface ILogin {
	void showLogin();

	void addListener(LoginListener l);

	void removeListener(LoginListener l);

	interface LoginListener {
		void userAuthenticated();

		void userShutdownRequest();
	}
}
