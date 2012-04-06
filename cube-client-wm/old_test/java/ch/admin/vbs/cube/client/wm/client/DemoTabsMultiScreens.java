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

package ch.admin.vbs.cube.client.wm.client;

import ch.admin.vbs.cube.client.wm.client.impl.ClientFacade;
import ch.admin.vbs.cube.client.wm.client.impl.CubeActionListener;
import ch.admin.vbs.cube.client.wm.client.impl.CubeClient;
import ch.admin.vbs.cube.client.wm.client.impl.VmActionListener;
import ch.admin.vbs.cube.client.wm.client.impl.VmControl;
import ch.admin.vbs.cube.client.wm.client.impl.VmMonitor;
import ch.admin.vbs.cube.client.wm.mock.MockAlreadyOpenedSessionManager;
import ch.admin.vbs.cube.client.wm.mock.MockXrandr;
import ch.admin.vbs.cube.client.wm.ui.CubeUI;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.CubeAbstractAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmAbstractAction;
import ch.admin.vbs.cube.client.wm.ui.wm.WindowManager;
import ch.admin.vbs.cube.client.wm.ui.x.imp.XWindowManager;
import ch.admin.vbs.cube.client.wm.utils.CubeUIDefaults;
import ch.admin.vbs.cube.client.wm.utils.IoC;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.core.ILoginUI;
import ch.admin.vbs.cube.core.ISessionUI;
import ch.admin.vbs.cube.core.impl.CubeCore;

/**
 * This demo application is used to develop and test the user UI (tabs, pop-ups,
 * etc).
 * 
 * It setups a client with a fake session manager. This fake session manager
 * emulate a logged user (session is open) with dummy VM.
 * 
 */
public class DemoTabsMultiScreens {
	private IoC ioc = new IoC();

	public static void main(String[] args) {
		DemoTabsMultiScreens d = new DemoTabsMultiScreens();
		d.run();
	}

	private void run() {
		// init UI Default
		CubeUIDefaults.initDefaults();
		// create beans
		ioc.addBean(new MockXrandr());
		ioc.addBean(new CubeUI());
		ioc.addBean(new CubeClient());
		ioc.addBean(new ClientFacade());
		ioc.addBean(new VmControl());
		ioc.addBean(new VmMonitor());
		ioc.addBean(new VmActionListener());
		ioc.addBean(new CubeActionListener());
		ioc.addBean(XWindowManager.getInstance());
		ioc.addBean(new WindowManager());
		ioc.addBean(new CubeCore());
		ioc.addBean(new MockAlreadyOpenedSessionManager());
		// IoC
		ioc.setupDependenciesOnAllBeans();
		// exotic stuff (to be cleaned up)
		VmAbstractAction.addVmActionListener(ioc.getBean(VmActionListener.class));
		CubeAbstractAction.addCubeActionListener(ioc.getBean(CubeActionListener.class));
		// object's specific initialization
		ioc.getBean(IXrandr.class).start();
		ioc.getBean(XWindowManager.class).start();
		ioc.getBean(MockAlreadyOpenedSessionManager.class).start();
		ioc.getBean(ILoginUI.class).closeDialog();
		ioc.getBean(ISessionUI.class).showWorkspace(ioc.getBean(MockAlreadyOpenedSessionManager.class).getSessions().get(0));

	}
}
