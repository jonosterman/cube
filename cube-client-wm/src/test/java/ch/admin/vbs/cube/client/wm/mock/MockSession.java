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

package ch.admin.vbs.cube.client.wm.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeClassification;
import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.common.UuidGenerator;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.mock.MockIdentityToken;
import ch.admin.vbs.cube.core.usb.UsbDevice;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntry;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntry.DeviceEntryState;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntryList;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmModel;
import ch.admin.vbs.cube.core.vm.VmStatus;
import ch.admin.vbs.cube.core.vm.list.VmDescriptor;

public class MockSession implements ISession {
	private static final Logger LOG = LoggerFactory.getLogger(MockSession.class);
	private IIdentityToken id = new MockIdentityToken(UuidGenerator.generate());
	private VmModel vmModel;
	private UsbDeviceEntryList usbDevices;

	public MockSession() {
		vmModel = new VmModel();
		try {
			Vm vm = new Vm(new VmDescriptor());
			vm.setVmStatus(VmStatus.RUNNING);
			vm.getDescriptor().getRemoteCfg().setId(UuidGenerator.generate());
			vm.getDescriptor().getRemoteCfg().setClassification(CubeClassification.CONFIDENTIAL);
			vm.getDescriptor().getRemoteCfg().setDescription("description");
			vm.getDescriptor().getRemoteCfg().setDomain("domain");
			vm.getDescriptor().getRemoteCfg().setName("Some VM here");
			vmModel.addVm(vm);
			usbDevices = new UsbDeviceEntryList();
			usbDevices.add(new UsbDeviceEntry(vm.getId(), new UsbDevice("0011", "ACME shop", "1010", "BuzzFlash"), DeviceEntryState.ALREADY_ATTACHED));
			usbDevices.add(new UsbDeviceEntry(vm.getId(), new UsbDevice("0012", "ACME shop", "1012", "Smash Balls"), DeviceEntryState.AVAILABLE));
			usbDevices.add(new UsbDeviceEntry(vm.getId(), new UsbDevice("0013", "Monkey Island Corp.", "1014", "Faggots"),
					DeviceEntryState.ATTACHED_TO_ANOTHER_VM));
			usbDevices.add(new UsbDeviceEntry(vm.getId(), new UsbDevice("0014", "Monkey Island Corp.", "1016", "Twitts"), DeviceEntryState.ALREADY_ATTACHED));
			usbDevices.add(new UsbDeviceEntry(vm.getId(), new UsbDevice("0015", "ACME shop", "1018", "The whole Internet"), DeviceEntryState.AVAILABLE));

			vm = new Vm(new VmDescriptor());
			vm.setVmStatus(VmStatus.STOPPED);
			vm.getDescriptor().getRemoteCfg().setId(UuidGenerator.generate());
			vm.getDescriptor().getRemoteCfg().setClassification(CubeClassification.CONFIDENTIAL);
			vm.getDescriptor().getRemoteCfg().setDescription("description");
			vm.getDescriptor().getRemoteCfg().setDomain("domain");
			vm.getDescriptor().getRemoteCfg().setName("Some BB Zm");
			vmModel.addVm(vm);
			vm = new Vm(new VmDescriptor());
			vm.setVmStatus(VmStatus.STOPPED);
			vm.getDescriptor().getRemoteCfg().setId(UuidGenerator.generate());
			vm.getDescriptor().getRemoteCfg().setClassification(CubeClassification.CONFIDENTIAL);
			vm.getDescriptor().getRemoteCfg().setDescription("description");
			vm.getDescriptor().getRemoteCfg().setDomain("domain");
			vm.getDescriptor().getRemoteCfg().setName("Some AA zm");
			vmModel.addVm(vm);
			vm = new Vm(new VmDescriptor());
			vm.setVmStatus(VmStatus.STOPPED);
			vm.getDescriptor().getRemoteCfg().setId(UuidGenerator.generate());
			vm.getDescriptor().getRemoteCfg().setClassification(CubeClassification.CONFIDENTIAL);
			vm.getDescriptor().getRemoteCfg().setDescription("description");
			vm.getDescriptor().getRemoteCfg().setDomain("domain");
			vm.getDescriptor().getRemoteCfg().setName("Some aa vm");
			vmModel.addVm(vm);
			vm = new Vm(new VmDescriptor());
			vm.setVmStatus(VmStatus.STOPPED);
			vm.getDescriptor().getRemoteCfg().setId(UuidGenerator.generate());
			vm.getDescriptor().getRemoteCfg().setClassification(CubeClassification.CONFIDENTIAL);
			vm.getDescriptor().getRemoteCfg().setDescription("description");
			vm.getDescriptor().getRemoteCfg().setDomain("domain");
			vm.getDescriptor().getRemoteCfg().setName("Some bb vm");
			vmModel.addVm(vm);
			

			vm = new Vm(new VmDescriptor());
			vm.setVmStatus(VmStatus.STOPPED);
			vm.getDescriptor().getRemoteCfg().setId(UuidGenerator.generate());
			vm.getDescriptor().getRemoteCfg().setClassification(CubeClassification.SECRET);
			vm.getDescriptor().getRemoteCfg().setDescription("description");
			vm.getDescriptor().getRemoteCfg().setDomain("domain");
			vm.getDescriptor().getRemoteCfg().setName("Some ZZ vm");
			vmModel.addVm(vm);

			vm = new Vm(new VmDescriptor());
			vm.setVmStatus(VmStatus.STOPPED);
			vm.getDescriptor().getRemoteCfg().setId(UuidGenerator.generate());
			vm.getDescriptor().getRemoteCfg().setClassification(CubeClassification.UNCLASSIFIED);
			vm.getDescriptor().getRemoteCfg().setDescription("description");
			vm.getDescriptor().getRemoteCfg().setDomain("domain");
			vm.getDescriptor().getRemoteCfg().setName("Some ZZ vm");
			vmModel.addVm(vm);

			vm = new Vm(new VmDescriptor());
			vm.setVmStatus(VmStatus.STOPPED);
			vm.getDescriptor().getRemoteCfg().setId(UuidGenerator.generate());
			vm.getDescriptor().getRemoteCfg().setClassification(CubeClassification.RESTRICTED);
			vm.getDescriptor().getRemoteCfg().setDescription("description");
			vm.getDescriptor().getRemoteCfg().setDomain("domain");
			vm.getDescriptor().getRemoteCfg().setName("Some ZZ vm");
			vmModel.addVm(vm);

			
		} catch (CubeException e) {
			e.printStackTrace();
		}
	}

	@Override
	public IIdentityToken getId() {
		return id;
	}

	@Override
	public void setId(IIdentityToken id) {
		this.id = id;
	}

	@Override
	public void close() {
	}

	@Override
	public void setContainerFactory(IContainerFactory containerFactory) {
	}

	@Override
	public void lock() {
	}

	@Override
	public void open() {
	}

	@Override
	public VmModel getModel() {
		return vmModel;
	}

	@Override
	public void controlVm(String vmId, VmCommand cmd, IOption option) {
		switch (cmd) {
		case LIST_USB:
			UsbDeviceEntryList list = (UsbDeviceEntryList) option;
			list.addAll(usbDevices);
			break;
		case ATTACH_USB: {
			UsbDevice d = (UsbDevice) option;
			for (int i = 0; i < usbDevices.size(); i++) {
				if (usbDevices.get(i).getDevice().getId().equals(d.getId())) {
					System.out.println("replace  attached");
					usbDevices.set(i, new UsbDeviceEntry(vmId, usbDevices.get(i).getDevice(), DeviceEntryState.ALREADY_ATTACHED));
				}
			}
			break;
		}
		case DETACH_USB: {
			UsbDevice d = (UsbDevice) option;
			for (int i = 0; i < usbDevices.size(); i++) {
				if (usbDevices.get(i).getDevice().getId().equals(d.getId())) {
					System.out.println("replace  free");
					usbDevices.set(i, new UsbDeviceEntry(vmId, usbDevices.get(i).getDevice(), DeviceEntryState.AVAILABLE));
				}
			}
			break;
		}
		default:
			LOG.debug("not implemented [{}]", cmd);
			break;
		}
	}
}
