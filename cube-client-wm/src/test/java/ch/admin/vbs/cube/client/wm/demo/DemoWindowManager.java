package ch.admin.vbs.cube.client.wm.demo;

import ch.admin.vbs.cube.client.wm.demo.swm.SimpleWindowManager;
import ch.admin.vbs.cube.client.wm.demo.swm.XSimpleWindowManager;

public class DemoWindowManager {
	public static void main(String[] args) throws Exception {
		// start Xephyr
		ProcessBuilder pb1 = new ProcessBuilder("Xephyr", "-ac", "-host-cursor", "-screen", "1280x1024", "-br", "-reset", ":9");
		pb1.start();
		Thread.sleep(500);
		// Simple Window Manager
		XSimpleWindowManager xswm = new XSimpleWindowManager();
		xswm.setDisplayName(":9");
		xswm.start();
		
		
		
		// start xclock
		Thread.sleep(500);
		ProcessBuilder pb2 = new ProcessBuilder("xclock");
		pb2.environment().put("DISPLAY", ":9");
		pb2.start();
		// use our window manager
		System.out.println("done.");
	}
}
