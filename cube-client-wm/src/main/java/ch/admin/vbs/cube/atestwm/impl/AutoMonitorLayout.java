package ch.admin.vbs.cube.atestwm.impl;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.IMonitorLayout;
import ch.admin.vbs.cube.atestwm.IMonitorLayout.IMonitorLayoutListener;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;

public class AutoMonitorLayout implements IMonitorLayout {
	private static final Logger LOG = LoggerFactory.getLogger(AutoMonitorLayout.class);
	private IXrandr xrandr;
	private ArrayList<IMonitorLayoutListener> listeners = new ArrayList<IMonitorLayout.IMonitorLayoutListener>(2);
	private Executor exec = Executors.newCachedThreadPool();

	@Override
	public void addListener(IMonitorLayoutListener l) {
		listeners.add(l);
	}
	@Override
	public void removeListener(IMonitorLayoutListener l) {
		listeners.remove(l);
	}
	private void fireLayoutChanged() {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				for(IMonitorLayoutListener l : listeners) {
					l.layoutChanged();
				}
			}
		});
	}
	
	public void setup(IXrandr xrandr) {
		this.xrandr = xrandr;
	}

	@Override
	public void pack() {
		// re-layout monitors
		int x = 0;
		for (XRScreen s : xrandr.getScreens()) {
			if (s.getState() == State.CONNECTED_AND_ACTIVE) {
				xrandr.setScreen(s, true, x, 0);
				x += s.getCurrentWidth();
			}
		}
		LOG.debug("Re-layout monitor(s) complete. Notify listeners [{}].",listeners.size());
		// refresh xrandr intern state and notify listeners
		xrandr.reloadConfiguration();
		fireLayoutChanged();
	}

	public void start() {
	}
}
