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
package ch.admin.vbs.cube.client.wm.demo;

import ch.admin.vbs.cube.atestwm.IXrandrMonitor;
import ch.admin.vbs.cube.atestwm.impl.AutoMonitorLayout;
import ch.admin.vbs.cube.atestwm.impl.TabManager;
import ch.admin.vbs.cube.atestwm.impl.XSimpleWindowManager;
import ch.admin.vbs.cube.atestwm.impl.XrandrMonitor;
import ch.admin.vbs.cube.client.wm.demo.swm.DemoMonitorControl;
import ch.admin.vbs.cube.client.wm.utils.IoC;

public class DemoWindowManager {
	public static void main(String[] args) throws Exception {
		// start Xephyr
		// ProcessBuilder pb1 = new ProcessBuilder("Xephyr", "-ac",
		// "-host-cursor", "-screen", "1280x1024", "-br", "-reset", ":9");
		ProcessBuilder pb1 = new ProcessBuilder("Xephyr", "-ac", "-host-cursor", "-screen", "640x480", "-br", "-reset", ":9");
		pb1.start();
		Thread.sleep(500);
		// Simple Window Manager

		//-------------------
		IoC ioc = new IoC();
		XSimpleWindowManager xswm = new XSimpleWindowManager();
		xswm.setDisplayName(":9");
		ioc.addBean(xswm);		
		ioc.addBean(new DemoMonitorControl());		
		ioc.addBean(new XrandrMonitor());		
		ioc.addBean(new AutoMonitorLayout());		
		ioc.addBean(new TabManager());
		//
		ioc.setupDependenciesOnAllBeans();
		//
		ioc.startAllBeans();
		//-------------------

		
		// start xclock
		Thread.sleep(500);
//		ProcessBuilder pb2 = new ProcessBuilder("gedit", "--new-window");
//		pb2.environment().put("DISPLAY", ":9");
//		pb2.start();
		// use our window manager
		System.out.println("done.");
	}
}
