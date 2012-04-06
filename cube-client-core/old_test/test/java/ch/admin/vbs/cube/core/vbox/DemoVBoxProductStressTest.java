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

package ch.admin.vbs.cube.core.vbox;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmModel;
import ch.admin.vbs.cube.core.vm.list.VmDescriptor;
import ch.admin.vbs.cube.core.vm.list.VmDescriptor.RemoteConfig;
import ch.admin.vbs.cube.core.vm.vbox.VBoxConfig;
import ch.admin.vbs.cube.core.vm.vbox.VBoxConfig.VBoxOption;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

public class DemoVBoxProductStressTest {
	private ArrayList<Vm> vms = new ArrayList<Vm>();
	private VBoxProduct product;
	private VmModel model;

	public static void main(String[] args) throws Exception {
		DemoVBoxProductStressTest d = new DemoVBoxProductStressTest();
		d.connectVirtualBoxWebService();
		d.prepareVms(10);
		//
		d.lowStressRegisterTest();
		d.middleStressRegisterTest();
		d.middleStressRegisterMultiThreadTest();
		//
		System.out.println("Completed");
	}

	private void middleStressRegisterMultiThreadTest() {
		final HashSet<String> registeredVms = new HashSet<String>();
		final HashSet<String> startedVms = new HashSet<String>();
		final Random r = new Random(System.currentTimeMillis());
		final int MAX = 25;
		for (int t = 0; t < 10; t++) {
			Thread th = new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Start test thread..");
					for (int i = 0; i < MAX; i++) {
						System.out.println("Round [" + i + "]..");
						synchronized (registeredVms) {
							try {
								// pick a VM (random)
								int x = r.nextInt(vms.size());
								Vm vm = vms.get(x);
								// // ----------------------------
								// try {
								// product.registerVm(vm);
								// product.startVm(vm);
								// Thread.sleep(3000);
								// product.save(vm, model);
								// product.unregisterVm(vm);
								// } catch (Exception e) {
								// e.printStackTrace();
								// }
								// System.exit(0);
								// // ----------------------------
								// register or unregister
								if (registeredVms.contains(vm.getId())) {
									//
									if (startedVms.contains(vm.getId())) {
										// registered + started
										startedVms.remove(vm.getId());
										if (r.nextBoolean()) {
											// -> save
											product.save(vm, model);
										} else {
											// -> stop
											product.poweroffVm(vm, model);
										}
									} else {
										// registered + not started
										if (r.nextBoolean()) {
											// -> start
											startedVms.add(vm.getId());
											product.startVm(vm);
											Thread.sleep(4000);
										} else {
											// -> unregister
											System.out.printf("Unregister VM [%s]\n", vm.getDescriptor().getRemoteCfg().getId());
											product.unregisterVm(vm);
											registeredVms.remove(vm.getId());
										}
									}
								} else {
									System.out.printf("Register VM [%s]\n", vm.getDescriptor().getRemoteCfg().getId());
									product.registerVm(vm);
									registeredVms.add(vm.getId());
								}
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(0);
							}
						}
					}
				}
			});
			th.start();
		}
	}

	private void lowStressRegisterTest() throws Exception {
		// register and unregister all vms
		for (Vm vm : vms) {
			System.out.printf("Register VM [%s]\n", vm.getDescriptor().getRemoteCfg().getId());
			product.registerVm(vm);
		}
		Thread.sleep(2000);
		for (Vm vm : vms) {
			System.out.printf("Unregister VM [%s]\n", vm.getDescriptor().getRemoteCfg().getId());
			product.unregisterVm(vm);
		}
	}

	private void middleStressRegisterTest() throws Exception {
		HashSet<String> registeredVms = new HashSet<String>();
		Random r = new Random(System.currentTimeMillis());
		int MAX = 50;
		for (int i = 0; i < MAX; i++) {
			System.out.println("Round [" + i + "/" + MAX + "]");
			// pick a VM (random)
			int x = r.nextInt(vms.size());
			Vm vm = vms.get(x);
			// register or unregister it
			if (registeredVms.remove(vm.getId())) {
				System.out.printf("Unregister VM [%s]\n", vm.getDescriptor().getRemoteCfg().getId());
				product.unregisterVm(vm);
			} else {
				System.out.printf("Register VM [%s]\n", vm.getDescriptor().getRemoteCfg().getId());
				product.registerVm(vm);
			}
		}
		// unregister all resting vms
		for (Vm vm : vms) {
			if (registeredVms.contains(vm.getId())) {
				System.out.printf("(cleanup) Unegister VM [%s]\n", vm.getDescriptor().getRemoteCfg().getId());
				product.unregisterVm(vm);
			}
		}
	}

	private ArrayList<Vm> prepareVms(int count) throws Exception {
		model = new VmModel();
		for (int i = 0; i < count; i++) {
			VmDescriptor d = new VmDescriptor();
			RemoteConfig r = d.getRemoteCfg();
			r.setId(String.format("00000000-0000-0000-0000-%012d", i));
			r.setName(r.getId());
			Vm vm = new Vm(d);
			vm.setExportFolder(new File(System.getProperty("java.io.tmpdir")));
			vm.setImportFolder(new File(System.getProperty("java.io.tmpdir")));
			File templateDisk = new File(getClass().getClassLoader().getResource("NewHardDisk1.vdi").getFile());
			File vmDir = new File(System.getProperty("java.io.tmpdir"), "DemoVBoxProductStressTest-" + i + "_vm");
			File rtDir = new File(System.getProperty("java.io.tmpdir"), "DemoVBoxProductStressTest-" + i + "_rt");
			vmDir.mkdirs();
			rtDir.mkdirs();
			File vmDisk = new File(vmDir, "disk1");
			// copy template and chage uuid
			ShellUtil su = new ShellUtil();
			su.run(new File("/"), 0, "cp", "-f", templateDisk.getAbsolutePath(), vmDisk.getAbsolutePath());
			su.run(new File("/"), 0, "VBoxManage", "internalcommands", "sethduuid", vmDisk.getAbsolutePath());
			Container vc = new Container();
			vc.setMountpoint(vmDir);
			Container rc = new Container();
			rc.setMountpoint(rtDir);
			vm.setVmContainer(vc);
			vm.setRuntimeContainer(rc);
			VBoxConfig cfg = new VBoxConfig(vc, rc);
			// vbox config
			cfg.setOption(VBoxOption.Disk1File, "disk1");
			cfg.setOption(VBoxOption.OsType, "Ubuntu");
			cfg.setOption(VBoxOption.IoApic, "true");
			cfg.setOption(VBoxOption.Acpi, "true");
			cfg.setOption(VBoxOption.Pae, "true");
			cfg.setOption(VBoxOption.BaseMemory, "1000");
			cfg.setOption(VBoxOption.VideoMemory, "128");
			cfg.setOption(VBoxOption.Nic1, "disabled");
			cfg.setOption(VBoxOption.Nic2, "disabled");
			cfg.setOption(VBoxOption.Nic3, "disabled");
			cfg.setOption(VBoxOption.Nic4, "disabled");
			cfg.setOption(VBoxOption.Nic1Bridge, "");
			cfg.setOption(VBoxOption.Nic2Bridge, "");
			cfg.setOption(VBoxOption.Nic3Bridge, "");
			cfg.setOption(VBoxOption.Nic4Bridge, "");
			cfg.save();
			//
			vms.add(vm);
			model.addVm(vm);
		}
		return vms;
	}

	private void connectVirtualBoxWebService() throws Exception {
		product = new VBoxProduct();
		product.start();
		while (!product.isConnected()) {
			System.out.println("Wait connection to VirtualBox web service..");
			Thread.sleep(1000);
		}
	}
}
