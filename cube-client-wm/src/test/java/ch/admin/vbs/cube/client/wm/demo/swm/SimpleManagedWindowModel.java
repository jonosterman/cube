package ch.admin.vbs.cube.client.wm.demo.swm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;


public class SimpleManagedWindowModel {
	public enum WindowType { MANAGED, BORDER }	
	
	private Lock lock = new ReentrantLock();
	private HashSet<Window> clientWindows = new HashSet<Window>();
	private HashSet<Window> borderWindows = new HashSet<Window>();
	private HashMap<Window,Window> border2ClientMap = new HashMap<Window, Window>();
	private HashMap<Window,Window> client2BorderMap = new HashMap<Window, Window>();
	
	public SimpleManagedWindowModel() {
	}
	
	public boolean isManaged(Window clientWindow) {
		lock.lock();
		try {
			return clientWindows.contains(clientWindow);
		} finally {
			lock.unlock();
		}
	}

	public void register(Window borderWindow, Window clientWindow) {
		lock.lock();
		try {
			borderWindows.add(borderWindow);
			clientWindows.add(clientWindow);
			border2ClientMap.put(borderWindow, clientWindow);
			client2BorderMap.put(clientWindow, borderWindow);
		} finally {
			lock.unlock();
		}	
	}
}
