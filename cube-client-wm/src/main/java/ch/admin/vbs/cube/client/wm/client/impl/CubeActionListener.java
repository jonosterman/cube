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

package ch.admin.vbs.cube.client.wm.client.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.admin.vbs.cube.client.wm.client.ICubeActionListener;
import ch.admin.vbs.cube.core.ICoreFacade;
import ch.admin.vbs.cube.core.usb.UsbDevice;

public class CubeActionListener implements ICubeActionListener {
	private ICoreFacade coreFacade;
	private ExecutorService exec = Executors.newCachedThreadPool();

	@Override
	public void lockCube() {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.lockCube();
			}
		});
	}

	@Override
	public void logoutUser() {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.logoutUser();
			}
		});
	}

	@Override
	public void shutdownMachine() {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.shutdownMachine();
			}
		});
	}

	@Override
	public void enteredPassword(final char[] password, final String requestId) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.enteredPassword(password, requestId);
			}
		});
	}

	@Override
	public void enteredConfirmation(final int result, final String requestId) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.enteredConfirmation(result, requestId);
			}
		});
	}

	@Override
	public void enteredUsbDevice(final UsbDevice device, final String requestId) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.enteredUsbDevice(device, requestId);
			}
		});
	}

	public void setup(ICoreFacade coreFacade) {
		this.coreFacade = coreFacade;
	}

	public ICoreFacade getCoreFacade() {
		return coreFacade;
	}
}
