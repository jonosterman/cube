
package ch.admin.vbs.cube.core.dbus;

import org.freedesktop.NMApplet;
import org.freedesktop.NetworkManager;
import org.freedesktop.NetworkManager.StateChanged;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;

import ch.admin.vbs.cube.core.network.impl.CNMStateMachine.StateChangedHandler;

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
