/**
 * Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package ch.admin.vbs.cube.client.wm.client;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.impl.ClientFacade;
import ch.admin.vbs.cube.client.wm.client.impl.CubeActionListener;
import ch.admin.vbs.cube.client.wm.client.impl.CubeClient;
import ch.admin.vbs.cube.client.wm.client.impl.VmActionListener;
import ch.admin.vbs.cube.client.wm.client.impl.VmControl;
import ch.admin.vbs.cube.client.wm.client.impl.VmMonitor;
import ch.admin.vbs.cube.client.wm.mock.MockAlreadyOpenedSessionManager;
import ch.admin.vbs.cube.client.wm.ui.IWindowsControl;
import ch.admin.vbs.cube.client.wm.ui.tabs.NavigationBar;
import ch.admin.vbs.cube.client.wm.ui.tabs.NavigationFrame;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.CubeAbstractAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmAbstractAction;
import ch.admin.vbs.cube.client.wm.ui.wm.BackgroundFrame;
import ch.admin.vbs.cube.client.wm.ui.wm.WindowManager;
import ch.admin.vbs.cube.client.wm.ui.x.imp.XWindowManager;
import ch.admin.vbs.cube.core.impl.CubeCore;

public class DemoTabsTwoScreens extends AbstractCubeDemoNewLogin {
	private static final Logger LOG = LoggerFactory.getLogger(DemoTabsTwoScreens.class);

	@Override
	public void initScreens() {
		JFrame display0 = new JFrame("cube frame");
		display0.setPreferredSize(new Dimension(800, 650));
		display0.setUndecorated(true);
		display0.pack();
		JFrame display1 = new JFrame("cube frame");
		display1.setPreferredSize(new Dimension(900, 700));
		display1.setUndecorated(true);
		display1.pack();
		// setup displays: get single screen or the nvidia twin view
		// screens, other things will not work
		int monitorCount = 2;
		cubeFrames = new JFrame[monitorCount];
		navBars = new NavigationBar[monitorCount];
		navBarFrames = new NavigationFrame[monitorCount];
		for (int i = 0; i < monitorCount; ++i) {
			// monitor dimension
			Rectangle monitorDim = null;
			if (i == 0) {
				monitorDim = new Rectangle(50, 50, display0.getWidth(), display0.getHeight());
			} else {
				monitorDim = new Rectangle(1000, 150, display1.getWidth(), display1.getHeight());
			}
			// create Cube Frames (each frame cover a monitor)
			cubeFrames[i] = new BackgroundFrame(i, monitorDim);
			// create navigation bar
			navBars[i] = new NavigationBar(i, monitorCount, cubeFrames[i]);
			navBarFrames[i] = navBars[i].getNavBar();
		}
	}

	public void initObjects() throws Exception {
		cubeClient = new CubeClient();
		clientFacade = new ClientFacade();
		vmControl = new VmControl();
		vmMonitor = new VmMonitor();
		vmActionListener = new VmActionListener();
		cubeActionListener = new CubeActionListener();
		xWindowManager = XWindowManager.getInstance();
		windowManager = new WindowManager();
		vmChangeListener = (IVmChangeListener) windowManager;
		windowsControl = (IWindowsControl) windowManager;
		//
		((WindowManager) windowManager).setParentFrame(cubeFrames);
		((WindowManager) windowManager).setNavigationFrames(navBarFrames);
		((ClientFacade) clientFacade).setupDependencies(cubeClient, windowManager);
		//
		smanager = new MockAlreadyOpenedSessionManager();
		//
		CubeCore core = new CubeCore();
		coreFacade = core;
		sessionUI = core;
		loginUI = core;

	}

	public void initDependencies() {
		cubeClient.addListener(vmChangeListener);
		// dependencies (UI / Client)
		((VmControl) vmControl).setupDependencies(cubeClient, windowsControl);
		((VmMonitor) vmMonitor).setupDependencies(cubeClient);
		((VmActionListener) vmActionListener).setupDependencies(coreFacade);
		((CubeActionListener) cubeActionListener).setupDependencies(coreFacade);
		for (NavigationBar n : navBars) {
			n.setup(cubeClient, coreFacade, vmControl, vmMonitor);
			cubeClient.addListener(n);
		}
		((WindowManager) windowManager).setup(cubeClient, cubeActionListener, vmMonitor, xWindowManager);
		// listeners
		VmAbstractAction.addVmActionListener(vmActionListener);
		CubeAbstractAction.addCubeActionListener(cubeActionListener);
		// dependencies (Core)
		((MockAlreadyOpenedSessionManager) smanager).setup(sessionUI);
		((CubeCore) coreFacade).setup(clientFacade, smanager);
	}

	public void initDemo() throws Exception {
		LOG.debug("Init: Show Frames");
		for (JFrame f : cubeFrames) {
			f.setVisible(true);
		}
		LOG.debug("Init: Start LoginMachine");
		//
		smanager.start();
		loginUI.closeDialog();
		sessionUI.showWorkspace(smanager.getSessions().get(0));
	}

	public static void main(String[] args) throws Exception {
		DemoTabsTwoScreens d = new DemoTabsTwoScreens();
		d.startDemo();
	}
}
