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

import java.awt.Dimension;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.impl.AutoMonitorLayout;
import ch.admin.vbs.cube.atestwm.impl.ScreenManager;
import ch.admin.vbs.cube.atestwm.impl.TabManager;
import ch.admin.vbs.cube.atestwm.impl.XSimpleWindowManager;
import ch.admin.vbs.cube.atestwm.impl.XrandrMonitor;
import ch.admin.vbs.cube.client.wm.utils.IoC;
import ch.admin.vbs.cube3.core.impl.VMMgr;
import ch.admin.vbs.cube3.core.impl.VBoxMgr;

public class DemoCubeWM {
	private static final Logger LOG = LoggerFactory.getLogger(DemoCubeWM.class);

	public static void main(String[] args) throws Exception {
		// start Xephyr if not started
		// ! Xephyr MUST be started on DIAPLAY :9 before running this
		// Xephyr -ac -host-cursor -screen 640x480 -br -reset :9
		// ! this application must be started with env DISPLAY=:9
		// Simple Window Manager
		// ------------------- champ europ course traineau
		JFrame f = new JFrame();
		f.getContentPane().setPreferredSize(new Dimension(100, 100));
		f.pack();
		f.setVisible(true);
		LOG.info("Init Cube..");
		IoC ioc = new IoC();
		ioc.addBean(new XSimpleWindowManager());
		ioc.addBean(new MockXrandr());
		ioc.addBean(new XrandrMonitor());
		ioc.addBean(new AutoMonitorLayout());
		ioc.addBean(new TabManager());
		ioc.addBean(new ScreenManager());
		ioc.addBean(new VMMgr());
		ioc.addBean(new VBoxMgr());
		//
		ioc.setupDependenciesOnAllBeans();
		LOG.info("Start Cube..");
		ioc.startAllBeans();
		// -------------------
		System.out.println("done.");
	}
}
