/**
 * Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.admin.vbs.cube.atestwm.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ch.admin.vbs.cube.atestwm.impl.ManagedWindow.WindowType;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public class SimpleManagedWindowModel {
	private Lock lock = new ReentrantLock();
	private HashMap<Window, ManagedWindow> clientWindows = new HashMap<Window, ManagedWindow>();
	private HashMap<Window, ManagedWindow> borderWindows = new HashMap<Window, ManagedWindow>();
	private ArrayList<ManagedWindow> managed = new ArrayList<ManagedWindow>();

	public SimpleManagedWindowModel() {
	}

	public boolean isManaged(Window client) {
		lock.lock();
		try {
			return clientWindows.containsKey(client);
		} finally {
			lock.unlock();
		}
	}

	public void register(ManagedWindow m) {
		lock.lock();
		try {
			managed.add(m);
			if (m.getClient() != null) {
				clientWindows.put(m.getClient(), m);
			}
			if (m.getBorder() != null) {
				borderWindows.put(m.getBorder(), m);
			}
		} finally {
			lock.unlock();
		}
	}

	public ManagedWindow getManaged(WindowType type, String screenId) {
		lock.lock();
		try {
			for (ManagedWindow m : managed) {
				if (m.getType() == type && screenId.equals(m.getScreenId())) {
					return m;
				}
			}
			return null;
		} finally {
			lock.unlock();
		}
	}

	public List<ManagedWindow> getManaged(WindowType type) {
		lock.lock();
		ArrayList<ManagedWindow> list = new ArrayList<ManagedWindow>();
		try {
			for (ManagedWindow m : managed) {
				if (m.getType() == type) {
					list.add(m);
				}
			}
			return list;
		} finally {
			lock.unlock();
		}
		
	}

	public void remove(ManagedWindow m) {
		lock.lock();
		try {
			managed.remove(m);
			if (m.getClient() != null) {
				clientWindows.remove(m.getClient());
			}
			if (m.getBorder() != null) {
				borderWindows.remove(m.getBorder());
			}
		} finally {
			lock.unlock();
		}		
	}

	public ManagedWindow getManagedByClient(Window client) {
		lock.lock();
		try {
			return clientWindows.get(client);
		} finally {
			lock.unlock();
		}		
	}
}
