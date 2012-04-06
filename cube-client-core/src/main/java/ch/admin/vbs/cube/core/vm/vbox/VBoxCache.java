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

package ch.admin.vbs.cube.core.vm.vbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.ws.WebServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.virtualbox_4_1.IMachine;
import org.virtualbox_4_1.MachineState;
import org.virtualbox_4_1.VBoxException;

/**
 * This class cache and monitor VirtualBox machines. It raises events when a
 * machine state changed.
 * 
 * This class is needed since event notification is not available through the
 * VirtualBox web service API.
 */
public class VBoxCache implements Runnable {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(VBoxCache.class);
	public static final long REFRESH_RATE = 1000; // ms
	private boolean running;
	private Thread thread;
	private final VBoxProduct vprod;
	private HashMap<String, CacheEntry> cache = new HashMap<String, CacheEntry>();

	public VBoxCache(VBoxProduct vprod) {
		this.vprod = vprod;
	}

	public void start() {
		running = true;
		thread = new Thread(this, "VBoxCache");
		thread.start();
	}

	public void stop() {
		running = false;
	}

	@Override
	public void run() {
		while (running) {
			try {
				sync();
			} catch (WebServiceException e) {
				LOG.error(
						"Failed to synchronize machine list with VirtualBox. Reconnect",
						e);
				vprod.reconnect();
			} catch (VBoxException e) {
				LOG.error(
						"Failed to synchronize machine list with VirtualBox. Reconnect",
						e);
				vprod.reconnect();
			} catch (Exception e) {
				LOG.error(
						"Failed to synchronize machine list with VirtualBox.",
						e);
			}
			//
			try {
				Thread.sleep(REFRESH_RATE);
			} catch (Exception e) {
			}
		}
	}

	public MachineState getState(String id) {
		synchronized (cache) {
			CacheEntry en = cache.get(id);
			if (en == null) {
				return null;
			} else {
				return en.state;
			}
		}
	}

	private void sync() {
		// check removed VM
		HashSet<String> rkeys = new HashSet<String>(cache.keySet());
		// get list of machines
		List<IMachine> vmachines = vprod.getMachines();
		for (IMachine m : vmachines) {
			// is already in cache?
			CacheEntry cached = cache.get(m.getId());
			if (cached == null) {
				// not in cache -> add cache entry
				cached = new CacheEntry(m);
				cache.put(m.getId(), cached);
				// machine added
				vprod.notifyVmAdded(m);
			} else {
				// already in cache -> check changes
				if (!cached.state.equals(m.getState())) {
					// update cache entry
					cached.machine = m;
					MachineState old = cached.state;
					cached.state = m.getState();
					vprod.notifyVmStateChanged(m, old);
				}
			}
			// removed machine index
			rkeys.remove(m.getId());
		}
		// handle removed machine
		for (String s : rkeys) {
			IMachine m = cache.remove(s).machine;
			vprod.notifyVmRemoved(m);
		}
	}

	private class CacheEntry {
		private IMachine machine;
		private MachineState state;

		public CacheEntry(IMachine machine) {
			this.machine = machine;
			this.state = machine.getState();
		}
	}

	public static interface VBoxCacheListener {
		void notifyVmRemoved(IMachine m);

		void notifyVmStateChanged(IMachine machine, MachineState oldState);

		void notifyVmAdded(IMachine m);
	}
}
