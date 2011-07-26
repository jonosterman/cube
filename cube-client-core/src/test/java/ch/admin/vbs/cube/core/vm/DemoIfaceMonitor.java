
package ch.admin.vbs.cube.core.vm;

import ch.admin.vbs.cube.core.network.vpn.NicMonitor;
import ch.admin.vbs.cube.core.network.vpn.NicMonitor.NicChangeListener;

public class DemoIfaceMonitor {
	public static void main(String[] args) throws Exception {
		NicMonitor mon = new NicMonitor();
		mon.addListener(new NicChangeListener() {
			@Override
			public void nicChanged() {
				System.out.println("nic changed");
			}
		});
		mon.start();
	}
}
