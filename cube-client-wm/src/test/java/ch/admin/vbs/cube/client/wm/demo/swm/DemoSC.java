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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.utils.IoC;
import ch.admin.vbs.cube.core.impl.ScTokenDevice;
import ch.admin.vbs.cube3.core.impl.Login;
import ch.admin.vbs.cube3.core.mock.MockToken;

public class DemoSC {
	private static final Logger LOG = LoggerFactory.getLogger(DemoSC.class);

	public static void main(String[] args) throws Exception {
		// start Xephyr if not started
		// ! Xephyr MUST be started on DIAPLAY :9 before running this
		// Xephyr -ac -host-cursor -screen 640x480 -br -reset :9
		// ! this application must be started with env DISPLAY=:9
		// Simple Window Manager
		// ------------------- 
		LOG.info("Init Cube..");
		IoC ioc = new IoC();
		ioc.addBean(new ScTokenDevice());
		ioc.addBean(new Login());
		ioc.addBean(new MockToken());
		//
		ioc.setupDependenciesOnAllBeans();
		LOG.info("Start Cube..");
		ioc.startAllBeans();
		// -------------------
		System.out.println("done.");
	}
}
