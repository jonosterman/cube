package ch.admin.vbs.cube.atestwm.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public class MWindowModel {
	private ArrayList<MWindow> windows = new ArrayList<MWindow>();
	private HashMap<Long, MWindow> xwindowCache = new HashMap<Long, MWindow>();
	private Lock lock = new ReentrantLock();

	public void addWindow(MWindow mw) {
		lock.lock();
		windows.add(mw);
		xwindowCache.put(mw.getXWindow().longValue(), mw);
		lock.unlock();
	}

	public void removeWindow(MWindow mw) {
		lock.lock();
		windows.remove(mw);
		xwindowCache.remove(mw.getXWindow().longValue());
		lock.unlock();
	}

	public MWindow getMWindow(Window window) {
		lock.lock();
		try {
			return xwindowCache.get(window.longValue());
		} finally {
			lock.unlock();
		}
	}
	public MWindow getMWindowByClient(Window client) {
		lock.lock();
		try {
			for (MWindow mw: windows) {
				if (mw.getXclient()!=null && mw.getXclient().longValue() == client.longValue()) {
					return mw;
				}
			}
			return null;
		} finally {
			lock.unlock();
		}
	}
}
