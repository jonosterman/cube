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

package ch.admin.vbs.cube.core.login;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.core.IAuthModule;
import ch.admin.vbs.cube.core.IAuthModuleListener;
import ch.admin.vbs.cube.core.ILogin;
import ch.admin.vbs.cube.core.ILoginListener;
import ch.admin.vbs.cube.core.ILoginUI;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.impl.CallbackPin;
import ch.admin.vbs.cube.core.impl.LoginMachine;
import ch.admin.vbs.cube.core.impl.ScAuthModule;
import ch.admin.vbs.cube.core.impl.ScTokenDevice;

public class DemoSmartCardStressWithCallBackTest implements ILoginListener, ILoginUI {
	private static final Logger LOG = Logger.getLogger("DemoSmartCardStressTest");

	public static void main(String[] args) throws Exception {
		DemoSmartCardStressWithCallBackTest d = new DemoSmartCardStressWithCallBackTest();
	}

	private IAuthModule amod;
	private ITokenDevice td;
	private LoginMachine login;

	public DemoSmartCardStressWithCallBackTest() throws Exception {
		td = new ScTokenDevice();
		amod = new ScAuthModule();
		login = new LoginMachine();
		login.setup(amod,td, this);
		login.addListener(this);
		td.start();
		amod.start();
	}

	static Random rnd = new Random();

	@Override
	public void userAuthentified(IIdentityToken id) {
		System.out.println("user authentificated");
	}

	@Override
	public void userLocked(IIdentityToken id) {
		System.out.println("user locked");
	}

	@Override
	public void userLogedOut(IIdentityToken id) {
		System.out.println("user loged out");
	}
	
	@Override
	public void closeDialog() {
	}
	@Override
	public void showDialog(String message, LoginDialogType type) {
	}
	@Override
	public void showPinDialog(String message, CallbackPin callback) {
	}

	public class PasswordCB implements CallbackHandler {
		private final String uuid;

		public PasswordCB(String uuid) {
			this.uuid = uuid;
		}

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			try {
				Thread.sleep(rnd.nextInt(1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			((PasswordCallback) callbacks[0]).setPassword("123456".toCharArray());
		}
	}
}
