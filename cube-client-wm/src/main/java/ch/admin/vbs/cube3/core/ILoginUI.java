package ch.admin.vbs.cube3.core;

public interface ILoginUI {
	void addListener(ILoginUIListener l);

	void removeListener(ILoginUIListener l);

	public interface ILoginUIListener {
		void setPassword(char[] passwd);

		void shutdown();
	}
}
