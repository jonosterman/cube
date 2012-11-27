package ch.admin.vbs.cube3.core;

public interface ILoginUI {
	void addListener(ILoginUIListener l);

	void removeListener(ILoginUIListener l);

	public interface ILoginUIListener {
		void setPassord(char[] passwd);

		void shutdown();
	}
}
