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

package ch.admin.vbs.cube.client.wm.dialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.Test;

import ch.admin.vbs.cube.client.wm.ui.dialog.FileTransferWizard;
import ch.admin.vbs.cube.client.wm.ui.dialog.FileTransferWizardListener;
import ch.admin.vbs.cube.common.CubeClassification;
import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.common.UuidGenerator;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmState;
import ch.admin.vbs.cube.core.vm.list.VmDescriptor;

public class TransferWizardDemo {
	public static void main(String[] args) throws IOException {
		TransferWizardDemo demo = new TransferWizardDemo();
		demo.testDialog();
	}

	private Vm generateVm(String name, String domain, CubeClassification classification) {
		VmDescriptor descriptor = new VmDescriptor();
		descriptor.getRemoteCfg().setClassification(classification);
		descriptor.getRemoteCfg().setDomain(domain);
		descriptor.getRemoteCfg().setId(UuidGenerator.generate());
		descriptor.getRemoteCfg().setName(name);
		descriptor.getLocalCfg().setRuntimeContainerUid(UuidGenerator.generate());
		descriptor.getRemoteCfg().setType("VirtualBox");
		descriptor.getLocalCfg().setVmContainerUid(UuidGenerator.generate());
		Vm vm = new Vm(descriptor);
		vm.setVmState(VmState.RUNNING);
		return vm;
	}

	@Test
	@Ignore
	public void testDialog() throws IOException {
		Vm vm = generateVm("My VM", "example.org", CubeClassification.UNCLASSIFIED);
		File x = new File("/tmp/bob.txt");
		x.createNewFile();
		RelativeFile transferFileName = new RelativeFile(x, x.getParentFile());
		ArrayList<Vm> vmMap = new ArrayList<Vm>();
		vmMap.add(vm);
		for (int i = 0; i < 10; i++) {
			Vm ovm = generateVm("Other VM " + i, "example.org", CubeClassification.values()[i%4]);
			vmMap.add(ovm);
		}
		FileTransferWizardListener listener = new FileTransferWizardListener() {
			@Override
			public void fileTransfer(RelativeFile filename, Vm src, Vm dst) {
				System.out.println("fileTransfer not implemented");
				System.exit(0);
			}
			@Override
			public void cancelTransfer(RelativeFile filename, Vm sourceVm) {
				System.out.println("cancelTransfer not implemented");
				System.exit(0);
			}
		};
		FileTransferWizard w = new FileTransferWizard(vm, transferFileName, vmMap, listener);
		w.displayWizard();
		System.out.println("done.");
	}
}
