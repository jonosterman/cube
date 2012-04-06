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

package ch.admin.vbs.cube.core.dbus;

import org.freedesktop.NMApplet;
import org.freedesktop.NetworkManager;
import org.freedesktop.NetworkManager.StateChanged;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;

public class DBusDemo {
	public static void main(String[] args) throws Exception {
		NMApplet nmappet = new NMApplet();
		nmappet.connect();
		nmappet.addSignalHanlder(DBusConnection.SYSTEM, StateChanged.class, new StateChangedHandler());
		try { nmappet.enable(false); } catch (Exception e) {}
		try { nmappet.enable(true); } catch (Exception e) {}
		// nmappet.enable(true);
		System.out.println("done.");
	}

	public static class StateChangedHandler implements DBusSigHandler<NetworkManager.StateChanged> {
		public StateChangedHandler() {
		}

		@Override
		public void handle(NetworkManager.StateChanged signal) {
			System.out.println("Signal: " + signal);
		}
	}
}
