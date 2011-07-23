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

package ch.admin.vbs.cube.client.wm.ui.wm;

import java.util.Collection;
import java.util.HashMap;

import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.client.VmHandle;

public class OsdFrameManager {
	private static final int OSD_TIMEOUT = 2000;
	private Thread osdFadingThread;
	private Object osdSemaphore = new Object();
	private long osdTimestamp = System.currentTimeMillis();
	private OsdFrame[] osdFrames;
	private IVmMonitor vmMon;

	public OsdFrameManager(OsdFrame[] allOsds) {
		osdFrames = allOsds;
		// create thread that will hide OSD frames after a timeout
		osdFadingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						if (osdTimestamp < System.currentTimeMillis()) {
							// hide OSD frames
							for (OsdFrame f : osdFrames) {
								f.setVisible(false);
							}
							// wait until next next
							synchronized (osdSemaphore) {
								osdSemaphore.wait(5000);
							}
						} else {
							// timeout not reached.
							Thread.sleep(1000);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}, "OSD Fading Thread");
		osdFadingThread.setDaemon(true);
		osdFadingThread.start();
	}

	public void showOsdFrames() {
		// set timestamp
		osdTimestamp = System.currentTimeMillis() + OSD_TIMEOUT;
		// display all OSD
		for (OsdFrame f : osdFrames) {
			f.displayOsd();
		}
		// activate fadeout thread
		synchronized (osdSemaphore) {
			osdSemaphore.notifyAll();
		}
	}

	public void setVmMon(IVmMonitor vmMon) {
		this.vmMon = vmMon;
	}

	public void update(Collection<VmHandle> values) {
		HashMap<String, VmHandle> map = new HashMap<String, VmHandle>();
		for (VmHandle h : values) {
			map.put(h.getMonitorId(), h);
		}
		// update osd content
		for (int i = 0; i < osdFrames.length; i++) {
			VmHandle h = map.get(i);
			osdFrames[i].setVmHandle(h, vmMon);
		}
	}

	public void hideAll() {
		for (OsdFrame f : osdFrames) {
			f.setVisible(false);
		}
	}
}
