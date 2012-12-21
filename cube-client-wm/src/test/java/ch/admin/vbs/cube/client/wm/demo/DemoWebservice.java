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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.mock.MockNetworkManager;
import ch.admin.vbs.cube.client.wm.utils.IoC;
import ch.admin.vbs.cube.common.CubeCommonProperties;
import ch.admin.vbs.cube.common.container.impl.DmcryptContainerFactory;
import ch.admin.vbs.cube.core.network.INetManager.NetState;
import ch.admin.vbs.cube3.core.impl.SessionMgr;
import ch.admin.vbs.cube3.core.impl.StaticJKSLogin;
import ch.admin.vbs.cube3.core.impl.WSClientMgr;
import ch.admin.vbs.cube3.core.mock.MockLoginUI;
import ch.admin.vbs.cube3.core.mock.MockTokenDevice;

/**
 * Test WebService vm listing 
 */
public class DemoWebservice {
	private static final Logger LOG = LoggerFactory.getLogger(DemoWebservice.class);

	public static void main(String[] args) throws Exception {
		LOG.info("Init Cube..");
		// initalize directories
		new File(CubeCommonProperties.getProperty("cube.mountpoints.dir")).mkdirs();
		new File(CubeCommonProperties.getProperty("cube.keys.dir")).mkdirs();
		new File(CubeCommonProperties.getProperty("cube.containers.dir")).mkdirs();
		// cleanup old containers
		DmcryptContainerFactory.cleanup();
		// Beans
		IoC ioc = new IoC();
		ioc.addBean(new MockLoginUI());
		MockLoginUI.setMockPassword("123456"); // to be used with StaticJKSLogin
		ioc.addBean(new MockTokenDevice(1000000)); // lot of time 
		ioc.addBean(new StaticJKSLogin());
		ioc.addBean(new SessionMgr());
		ioc.addBean(new DmcryptContainerFactory());
		//
		ioc.addBean(new WSClientMgr());
		ioc.addBean(new MockNetworkManager(new NetState[] {NetState.CONNECTED_DIRECT},60000));
		
		
		//
		ioc.setupDependenciesOnAllBeans();
		LOG.info("Start Cube..");
		ioc.startAllBeans();
		// -------------------
		System.out.println("done.");
	}
}
