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
package ch.admin.vbs.cube.client.wm.demo.swm;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;

/**
 * monitor monitor configuration changes (monitor pluged in, monitor
 * activated/deactivated) and notify layout about it.
 */
public class XrandrMonitor implements Runnable, IXrandrMonitor {
	private static final Logger LOG = LoggerFactory.getLogger(XrandrMonitor.class);
	private IXrandr xrandr;
	private int configHash = "none".hashCode();
	private IMonitorLayout monitorLayout;

	public XrandrMonitor() {
	}

	public void setup(IXrandr xrandr, IMonitorLayout monitorLayout) {
		this.xrandr = xrandr;
		this.monitorLayout = monitorLayout;
	}

	@Override
	public void start() {
		Thread t = new Thread(this, "MonitorManager Thread");
		t.setDaemon(true);
		t.start();
	}

	@Override
	public void run() {
		while (true) {
			try {
				// monitormgr -> layout -> bg-sync ??? how does it chain? not
				// parallel.
				Thread.sleep(1000);
				StringBuffer sb = new StringBuffer();
				xrandr.reloadConfiguration();
				for (XRScreen s : xrandr.getScreens()) {
					sb.append(s.getId()).append(';');
					sb.append(s.getPosX()).append(';');
					sb.append(s.getPosY()).append(';');
					sb.append(s.getCurrentWidth()).append(';');
					sb.append(s.getCurrentHeight()).append(';');
					sb.append(s.getState()).append(';');
				}
				int newHash = sb.toString().hashCode();
				if (newHash != configHash) {
					// config has changed
					configHash = newHash;
					LOG.debug("Monitor configuration has changed. Re-layout them");
					monitorLayout.pack();
				}
			} catch (Exception e) {
			}
		}
	}
}
