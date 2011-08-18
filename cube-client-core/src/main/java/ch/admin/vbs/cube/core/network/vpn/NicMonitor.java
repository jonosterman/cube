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


package ch.admin.vbs.cube.core.network.vpn;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NicMonitor implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(NicMonitor.class);
	private boolean running;
	private ArrayList<NicChangeListener> listeners = new ArrayList<NicMonitor.NicChangeListener>(2);

	public void start() {
		Thread t = new Thread(this, "NicMonitor");
		t.start();
	}

	public void addListener(NicChangeListener l) {
		listeners.add(l);
	}

	public void removeListener(NicChangeListener l) {
		listeners.remove(l);
	}

	@Override
	public void run() {
		running = true;
		String ref = null;
		while (running) {
			StringBuffer sb = new StringBuffer();
			// list all interface
			try {
				Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
				while (en.hasMoreElements()) {
					NetworkInterface ni = en.nextElement();
					if (ni.getDisplayName().startsWith("eth")) {
						sb.append(ni.getDisplayName()).append('#');
						sb.append(ni.isUp()).append('{');
						Enumeration<InetAddress> adds = ni.getInetAddresses();
						while (adds.hasMoreElements()) {
							InetAddress addr = adds.nextElement();
							sb.append(addr.getHostAddress()).append(',');
						}
						sb.append("}\n");
					}
				}
				String challenge = sb.toString();
				if (ref == null) {
					ref = challenge;
				} else if (!ref.equals(challenge)) {
					fireChange();
					ref = challenge;
				}
			} catch (Exception e) {
				LOG.error("Failed to monitor NIC.", e);
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}
		}
	}

	private void fireChange() {
		for (NicChangeListener l : listeners) {
			l.nicChanged();
		}
	}

	public interface NicChangeListener {
		void nicChanged();
	}
}
