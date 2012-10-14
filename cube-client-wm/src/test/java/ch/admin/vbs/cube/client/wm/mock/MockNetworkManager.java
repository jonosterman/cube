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

import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;
import ch.admin.vbs.cube.core.network.INetManager;

public class MockNetworkManager implements INetManager {
	@Override
	public void start() {
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
			for(String line : su.getStandardOutput().toString().split("\n")) {
				if (line.startsWith("eth") | line.startsWith("wlan")) {
					list.add(line.split(" +",2)[0]);					
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
	}

	@Override
	public void removeListener(Listener l) {
	}
	
	@Override
	public NetState getState() {
		// TODO Auto-generated method stub
		return null;
	}
}
