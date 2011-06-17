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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.impl.ClientFacade;
import ch.admin.vbs.cube.client.wm.client.impl.CubeActionListener;
import ch.admin.vbs.cube.client.wm.client.impl.CubeClient;
import ch.admin.vbs.cube.client.wm.client.impl.VmActionListener;
import ch.admin.vbs.cube.client.wm.client.impl.VmControl;
import ch.admin.vbs.cube.client.wm.client.impl.VmMonitor;
import ch.admin.vbs.cube.client.wm.ui.IWindowsControl;
import ch.admin.vbs.cube.client.wm.ui.tabs.NavigationBar;
import ch.admin.vbs.cube.client.wm.ui.tabs.NavigationFrame;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.CubeAbstractAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.IVmActionListener;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmAbstractAction;
import ch.admin.vbs.cube.client.wm.ui.wm.BackgroundFrame;
import ch.admin.vbs.cube.client.wm.ui.wm.WindowManager;
import ch.admin.vbs.cube.client.wm.ui.x.imp.XWindowManager;
import ch.admin.vbs.cube.client.wm.utils.CubeUIDefaults;
import ch.admin.vbs.cube.common.CubeCommonProperties;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.container.impl.DmcryptContainerFactory;
import ch.admin.vbs.cube.core.IAuthModule;
import ch.admin.vbs.cube.core.IClientFacade;
import ch.admin.vbs.cube.core.ICoreFacade;
import ch.admin.vbs.cube.core.ILoginUI;
import ch.admin.vbs.cube.core.ISessionManager;
import ch.admin.vbs.cube.core.ISessionUI;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.impl.CubeCore;
import ch.admin.vbs.cube.core.impl.LoginMachine;
import ch.admin.vbs.cube.core.impl.ScAuthModule;
import ch.admin.vbs.cube.core.impl.ScTokenDevice;
import ch.admin.vbs.cube.core.impl.SessionManager;

public class AbstractCubeDemoNewLogin {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractCubeDemoNewLogin.class);
	// core
	protected ICoreFacade coreFacade;
	// client
	protected ICubeClient cubeClient;
	protected IClientFacade clientFacade;
	protected IVmControl vmControl;
	protected IVmMonitor vmMonitor;
	protected IVmActionListener vmActionListener;
	protected ICubeActionListener cubeActionListener;
	protected IXWindowManager xWindowManager;
	protected IUserInterface windowManager;
	protected IVmChangeListener vmChangeListener;
	protected IWindowsControl windowsControl;
	protected NavigationBar[] navBars;
	protected JFrame[] cubeFrames;
	protected NavigationFrame[] navBarFrames;
	// core
	protected LoginMachine login;
	protected ISessionManager smanager;
	protected IAuthModule authModule;
	protected ITokenDevice tokenDevice;
	protected IContainerFactory containerFactory;
	protected ILoginUI loginUI;
	protected ISessionUI sessionUI;

	public void initConfiguration() {
		// init UI Default
		CubeUIDefaults.initDefaults();
		// initalize directories
		new File(CubeCommonProperties.getProperty("cube.mountpoints.dir")).mkdirs();
		new File(CubeCommonProperties.getProperty("cube.keys.dir")).mkdirs();
		new File(CubeCommonProperties.getProperty("cube.containers.dir")).mkdirs();
		// cleanup older containers
		DmcryptContainerFactory.cleanup();
	}

	public void initScreens() {
		JFrame display0 = new JFrame("cube frame");
		display0.setPreferredSize(new Dimension(600, 480));
		display0.setUndecorated(true);
		display0.pack();
		JFrame display1 = new JFrame("cube frame");
		display1.setPreferredSize(new Dimension(800, 400));
		display1.setUndecorated(true);
		display1.pack();
		// setup displays: get single screen or the nvidia twin view
		// screens, other things will not work
		GraphicsEnvironment grapicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] graphicsDevice = grapicsEnvironment.getScreenDevices();
		int monitorCount = graphicsDevice.length;
		cubeFrames = new JFrame[monitorCount];
		navBars = new NavigationBar[monitorCount];
		navBarFrames = new NavigationFrame[monitorCount];
		LOG.debug("Init: create NavigationBar & BackgroundFrame");
		for (int i = 0; i < monitorCount; ++i) {
			// monitor dimension
			Rectangle monitorDim = new Rectangle(graphicsDevice[i].getDefaultConfiguration().getBounds());
			if (new File("/opt/cube/debug").exists()) {
				// DEBUG
				LOG.warn("HALF SIZE SCREEN FOR DEBUG PURPOSE"); // DEBUG
				monitorDim.x += 100; // DEBUG
				monitorDim.y += 100; // DEBUG
				monitorDim.width /= 2; // DEBUG
				monitorDim.height /= 2; // DEBUG
				// DEBUG
			}
			LOG.debug("Screen[{}] ({})", i, monitorDim.toString());
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
		login = new LoginMachine();
		smanager = new SessionManager();
		//
		authModule = new ScAuthModule();
		tokenDevice = new ScTokenDevice();
		containerFactory = new DmcryptContainerFactory();
		//
		CubeCore core = new CubeCore();
		coreFacade = core;
		sessionUI = core;
		loginUI = core;
	}

	public void initDependencies() {
		cubeClient.addListener(vmChangeListener);
		// dependencies (UI / Client)
		((VmControl)vmControl).setupDependencies(cubeClient, windowsControl);
		((VmMonitor)vmMonitor).setupDependencies(cubeClient);
		((VmActionListener)vmActionListener).setupDependencies(coreFacade);
		((CubeActionListener)cubeActionListener).setupDependencies(coreFacade);
		
		for (NavigationBar n : navBars) {
			n.setup(cubeClient, coreFacade, vmControl, vmMonitor);
			cubeClient.addListener(n);
		}
		((WindowManager)windowManager).setup(cubeClient, cubeActionListener, vmMonitor, xWindowManager);
		// listeners
		VmAbstractAction.addVmActionListener(vmActionListener);
		CubeAbstractAction.addCubeActionListener(cubeActionListener);
		// dependencies (Core)
		((LoginMachine)login).setup(authModule, tokenDevice, loginUI);
		((SessionManager)smanager).setup(login, sessionUI, containerFactory);
		((CubeCore)coreFacade).setup(clientFacade,smanager);
	}

	public void initDemo() throws Exception {
		LOG.debug("Init: Show Frames");
		for (JFrame f : cubeFrames) {
			f.setVisible(true);
		}
		LOG.debug("Init: Start LoginMachine");
		
		//
		smanager.start();
		tokenDevice.start();
		authModule.start();
		login.start();
//		windowManager.showMessageDialog("A critical error occurs. Please contact the administrator.", IClientFacade.OPTION_SHUTDOWN);
	}

	public void startDemo() throws Exception {
		initConfiguration();
		initScreens();
		initObjects();
		initDependencies();
		initDemo();
	}
}