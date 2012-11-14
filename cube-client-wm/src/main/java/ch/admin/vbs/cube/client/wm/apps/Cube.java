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

package ch.admin.vbs.cube.client.wm.apps;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.impl.AutoMonitorLayout;
import ch.admin.vbs.cube.atestwm.impl.ScreenManager;
import ch.admin.vbs.cube.atestwm.impl.TabManager;
import ch.admin.vbs.cube.atestwm.impl.XSimpleWindowManager;
import ch.admin.vbs.cube.atestwm.impl.XrandrMonitor;
import ch.admin.vbs.cube.client.wm.client.impl.ClientFacade;
import ch.admin.vbs.cube.client.wm.client.impl.CubeActionListener;
import ch.admin.vbs.cube.client.wm.client.impl.CubeClient;
import ch.admin.vbs.cube.client.wm.client.impl.VmActionListener;
import ch.admin.vbs.cube.client.wm.client.impl.VmControl;
import ch.admin.vbs.cube.client.wm.client.impl.VmMonitor;
import ch.admin.vbs.cube.client.wm.demo.swm.MockXrandr;
import ch.admin.vbs.cube.client.wm.ui.CubeUI;
import ch.admin.vbs.cube.client.wm.ui.ICubeUI;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.CubeAbstractAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmAbstractAction;
import ch.admin.vbs.cube.client.wm.ui.wm.WindowManager;
import ch.admin.vbs.cube.client.wm.ui.x.imp.XWindowManager;
import ch.admin.vbs.cube.client.wm.utils.CubeUIDefaults;
import ch.admin.vbs.cube.client.wm.utils.IoC;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.impl.XrandrCLI;
import ch.admin.vbs.cube.common.CubeCommonProperties;
import ch.admin.vbs.cube.common.MachineUuid;
import ch.admin.vbs.cube.common.container.impl.DmcryptContainerFactory;
import ch.admin.vbs.cube.core.IAuthModule;
import ch.admin.vbs.cube.core.ILogin;
import ch.admin.vbs.cube.core.ISessionManager;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.impl.CubeCore;
import ch.admin.vbs.cube.core.impl.LoginMachine;
import ch.admin.vbs.cube.core.impl.ScTokenDevice;
import ch.admin.vbs.cube.core.impl.SessionManager;
import ch.admin.vbs.cube.core.impl.scauthmodule.ScAuthModule;
import ch.admin.vbs.cube.core.network.INetManager;
import ch.admin.vbs.cube.core.network.impl.NetManager;
import ch.admin.vbs.cube.core.vm.IVmController;
import ch.admin.vbs.cube.core.vm.VmAudioControl;
import ch.admin.vbs.cube.core.vm.VmController;
import ch.admin.vbs.cube3.core.impl.VBoxMgr;
import ch.admin.vbs.cube3.core.impl.VMMgr;

/**
 * This class is the entry point to start the cube secure client.
 */
public final class Cube {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(Cube.class);
	// Beans
	private IoC ioc = new IoC();

	/**
	 * Starts the cube application.
	 * 
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		try {
			LOG.info("================================================================");
			LOG.info("================================================================");
			LOG.info("===                                                          ===");
			LOG.info("===                       CUBE STARTUP                       ===");
			LOG.info("===                                                          ===");
			LOG.info("================================================================");
			LOG.info("================================================================");
			LOG.info("");
			LOG.info(" Machine UUID: " + MachineUuid.getMachineUuid().getUuidAsString());
			LOG.info("");
			LOG.info("================================================================");
			for (Object k : System.getProperties().keySet()) {
				LOG.info(String.format("%s = %s",k, System.getProperty((String)k)).trim());
			}
			// #########################
			// Start application
			// #########################
			Cube d = new Cube();
			d.run();
		} catch (Exception e) {
			LOG.error("Failed to start cube", e);
		}
	}

	
	private void run() throws Exception {
		IoC ioc = new IoC();
		XSimpleWindowManager xswm = new XSimpleWindowManager();
		ioc.addBean(xswm);		
		ioc.addBean(new XrandrCLI());
		ioc.addBean(new XrandrMonitor());		
		ioc.addBean(new AutoMonitorLayout());		
		ioc.addBean(new TabManager());		
		ioc.addBean(new ScreenManager());
		ioc.addBean(new VMMgr());
		ioc.addBean(new VBoxMgr());
		
		//
		ioc.setupDependenciesOnAllBeans();
		//
		ioc.startAllBeans();
		
	}
	
	private void run_ori() throws Exception {
		// init UI Default
		new VmAudioControl().setMainVolume(120);
		CubeUIDefaults.initDefaults();
		// initalize directories
		new File(CubeCommonProperties.getProperty("cube.mountpoints.dir")).mkdirs();
		new File(CubeCommonProperties.getProperty("cube.keys.dir")).mkdirs();
		new File(CubeCommonProperties.getProperty("cube.containers.dir")).mkdirs();
		// cleanup older containers
		DmcryptContainerFactory.cleanup();

		// create beans
		ioc.addBean(new XrandrCLI());
		ioc.addBean(new CubeUI());
		ioc.addBean(new CubeClient());
		ioc.addBean(new ClientFacade());
		ioc.addBean(new VmControl());
		ioc.addBean(new VmMonitor());
		ioc.addBean(new VmActionListener());
		ioc.addBean(new CubeActionListener());
//		ioc.addBean(XWindowManager.getInstance());
		ioc.addBean(new WindowManager());
		ioc.addBean(new XWindowManager());
		ioc.addBean(new CubeCore());
		ioc.addBean(new VmController());
		ioc.addBean(new SessionManager());
		ioc.addBean(new LoginMachine());
		ioc.addBean(new ScAuthModule());
		ioc.addBean(new DmcryptContainerFactory());
		ioc.addBean(new ScTokenDevice());
		ioc.addBean(new NetManager());
		
		// IoC
		ioc.setupDependenciesOnAllBeans();
		
		// exotic stuff (to be cleaned up)
		VmAbstractAction.addVmActionListener(ioc.getBean(VmActionListener.class));
		CubeAbstractAction.addCubeActionListener(ioc.getBean(CubeActionListener.class));
		
		// object's specific initialization
		ioc.getBean(IXrandr.class).start();
		ioc.getBean(ICubeUI.class).start();
		ioc.getBean(XWindowManager.class).start();
		ioc.getBean(IVmController.class).start();
		ioc.getBean(INetManager.class).start();
		ioc.getBean(ISessionManager.class).start();
		ioc.getBean(ITokenDevice.class).start();
		ioc.getBean(IAuthModule.class).start();
		ioc.getBean(ILogin.class).start();
	}
}
