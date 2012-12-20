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
package ch.admin.vbs.cube.client.wm.mock;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;
import ch.admin.vbs.cube.core.network.INetManager;

public class MockNetworkManager implements INetManager, Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(MockNetworkManager.class);
	private ArrayList<Listener> listeners = new ArrayList<INetManager.Listener>();
	private int nStateIdx = 0;
	private NetState[] sequence;

	public MockNetworkManager() {
		sequence = new NetState[] { //
		NetState.DEACTIVATED, NetState.CONNECTING, NetState.CONNECTING_VPN, NetState.CONNECTED_BY_VPN, //
				NetState.DEACTIVATED, NetState.CONNECTING, NetState.CONNECTED_DIRECT };
	}

	@Override
	public void start() {
		Thread t = new Thread(this);
		t.setDaemon(true);
		t.start();
	}
	
	public void setup() {}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(10000);
				NetState old = getState();
				nStateIdx = (nStateIdx + 1) % sequence.length;
				NetState sta = getState();
				LOG.debug("Change Network State [{}]->[{}]",old,sta);
				// fore event
				for(Listener l : listeners) {
					l.stateChanged(old, sta);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
	}

	@Override
	public List<String> getNetworkInterfaces() {
		ArrayList<String> list = new ArrayList<String>();
		ShellUtil su = new ShellUtil();
		try {
			su.run(null, ShellUtil.NO_TIMEOUT, "ifconfig");
			for (String line : su.getStandardOutput().toString().split("\n")) {
				if (line.startsWith("eth") | line.startsWith("wlan")) {
					String iface = line.split(" +", 2)[0];
					list.add(iface);
					LOG.debug("Add iface [{}]", iface);
				}
			}
		} catch (ShellUtilException e) {
			e.printStackTrace();
		}
		return list;
	}

	public static void main(String[] args) {
		new MockNetworkManager().getNetworkInterfaces();
	}

	@Override
	public void addListener(Listener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(Listener l) {
		listeners.remove(l);
	}

	@Override
	public NetState getState() {
		return sequence[nStateIdx];
	}
}
