package ch.admin.vbs.cube.client.wm.demo.swm;

public interface IMonitorLayout {
	void pack();

	void addListener(IMonitorLayoutListener l);

	void removeListener(IMonitorLayoutListener l);

	public interface IMonitorLayoutListener {
		void layoutChanged();
	}
}
