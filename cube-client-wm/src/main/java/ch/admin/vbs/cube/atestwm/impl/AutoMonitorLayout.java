package ch.admin.vbs.cube.atestwm.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.IMonitorLayout;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;

public class AutoMonitorLayout implements IMonitorLayout {
	private final static boolean DEBUG_REVERSE_ORDER = false;
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
				for (IMonitorLayoutListener l : listeners) {
					l.layoutChanged();
				}
			}
		});
	}

	public void setup(IXrandr xrandr) {
		this.xrandr = xrandr;
	}

	@Override
	/** This method is called by XRandrMonitor when it detects a change in available monitors list. */
	public void pack() {
		// arbitrary re-layout monitors side by side.
		int x = 0;
		ArrayList<XRScreen> screenList = new ArrayList<XRScreen>(xrandr.getScreens());
		if (DEBUG_REVERSE_ORDER) {
			/**
			 * In practice, new screen may be added at the beginning of the list
			 * (or somewhere else). And it trigger some layout bugs (windows
			 * content was misplaced). In order to simulate that during tests
			 * (using only Xephyr server) we reverse this list.
			 */
			Collections.reverse(screenList);
		}
		// update x coordinate of all XRScreens. So they will be side by side.
		for (XRScreen s : screenList) {
			if (s.getState() == State.CONNECTED_AND_ACTIVE || s.getState() == State.CONNECTED) {
				// apply changes using xrandr
				xrandr.setScreen(s, true, x, 0);
				x += s.getCurrentWidth();
			}
		}
		// refresh xrandr intern state
		xrandr.reloadConfiguration();
		// notify listeners
		LOG.debug("Monitor(s) have been re-configured with new layout. Notify listeners [{}].", listeners.size());
		fireLayoutChanged();
	}

	public void start() {
		// nothing
	}
}
