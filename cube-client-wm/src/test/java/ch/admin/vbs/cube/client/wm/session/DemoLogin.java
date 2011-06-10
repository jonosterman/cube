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

package ch.admin.vbs.cube.client.wm.session;

import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.container.impl.DmcryptContainerFactory;
import ch.admin.vbs.cube.core.IAuthModule;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.impl.LoginMachine;
import ch.admin.vbs.cube.core.impl.ScAuthModule;
import ch.admin.vbs.cube.core.impl.ScTokenDevice;
import ch.admin.vbs.cube.core.impl.SessionManager;


public class DemoLogin {
	public static void main(String[] args) throws Exception {
		LoginMachine machine = new LoginMachine();
		SessionManager smanager = new SessionManager();
		//ITokenDevice tokenDevice = new JMockDevice();
		//IAuthModule authModule = new MockAuthModule();
		
		IAuthModule authModule = new ScAuthModule();
		ITokenDevice tokenDevice = new ScTokenDevice();
		IContainerFactory containerFactory = new DmcryptContainerFactory();
		//
//		ITerminal terminal = new JMockTerminal();
//		//ISessionUI sessionUI = new JMockSessionUI();
//		//
////		machine.setup(authModule, tokenDevice, terminal);
//		smanager.setup(machine, sessionUI, containerFactory);
//		((JMockTerminal)terminal).setupDependencies(sessionUI);
		//
		smanager.start();
		tokenDevice.start();
		authModule.start();
		machine.start();
	}
}
