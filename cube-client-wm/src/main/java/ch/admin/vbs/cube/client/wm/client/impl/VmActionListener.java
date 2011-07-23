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

import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.IVmActionListener;
import ch.admin.vbs.cube.core.ICoreFacade;
import ch.admin.vbs.cube.core.usb.UsbDevice;

public class VmActionListener implements IVmActionListener {
	private ICoreFacade coreFacade;
	private ExecutorService exec = Executors.newCachedThreadPool();

	public VmActionListener() {
	}

	@Override
	public void deleteVm(final VmHandle h) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.deleteVm(h.getVmId());
			}
		});
	}

	@Override
	public void poweroffVm(final VmHandle h) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.powerOffVm(h.getVmId());
			}
		});
	}

	@Override
	public void installGuestAdditions(final VmHandle h) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.installGuestAdditions(h.getVmId());
			}
		});
	}

	@Override
	public void stageVm(final VmHandle h) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.stageVm(h.getVmId(), null);
			}
		});
	}

	@Override
	public void saveVm(final VmHandle h) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.standByVm(h.getVmId());
			}
		});
	}

	@Override
	public void startVm(final VmHandle h) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				// request VM start to Core
				coreFacade.startVm(h.getVmId());
			}
		});
	}

	@Override
	public void connectUsbDevice(final VmHandle h) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.attachUsbDeviceRequest(h.getVmId());
			}
		});
	}

	@Override
	public void attachUsbDevice(final VmHandle h, final UsbDevice usb) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.attachUsbDevice(h.getVmId(), usb);
			}
		});
	}

	@Override
	public void detachUsbDevice(final VmHandle h, final UsbDevice usb) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.detachUsbDevice(h.getVmId(), usb);
			}
		});
	}
	
	@Override
	public void setVmProperty(final VmHandle h, final String key, final String value, final boolean refreshAllVms) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				coreFacade.setVmProperty(h.getVmId(), key, value, refreshAllVms);
			}
		});
	}

	public void setup(ICoreFacade coreFacade) {
		this.coreFacade = coreFacade;
	}
}
