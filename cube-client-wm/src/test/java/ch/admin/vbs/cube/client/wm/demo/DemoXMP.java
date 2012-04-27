package ch.admin.vbs.cube.client.wm.demo;

import ch.admin.vbs.cube.client.wm.ui.x.imp.XWindowManager;

public class DemoXMP {
	public static void main(String[] args) throws Exception {
		new DemoXMP().run();
	}

	private void run() throws Exception {
		XWindowManager wm = (XWindowManager) XWindowManager.getInstance();
		wm.start();
		//
		wm.debug_dumpWindows();
		
		//
		Thread.sleep(2000);
		wm.destroy();
		System.out.println("done");
	}
}
