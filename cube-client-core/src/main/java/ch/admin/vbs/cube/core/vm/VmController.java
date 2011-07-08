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
package ch.admin.vbs.cube.core.vm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeCommonProperties;
import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.ContainerFactoryProvider;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.core.ISession.IOption;
import ch.admin.vbs.cube.core.ISession.VmCommand;
import ch.admin.vbs.cube.core.network.vpn.VpnManager;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntryList;
import ch.admin.vbs.cube.core.vm.IVmProduct.VmProductState;
import ch.admin.vbs.cube.core.vm.ctrtasks.Delete;
import ch.admin.vbs.cube.core.vm.ctrtasks.InstallGuestAdditions;
import ch.admin.vbs.cube.core.vm.ctrtasks.PowerOff;
import ch.admin.vbs.cube.core.vm.ctrtasks.Save;
import ch.admin.vbs.cube.core.vm.ctrtasks.Start;
import ch.admin.vbs.cube.core.vm.ctrtasks.UsbAttach;
import ch.admin.vbs.cube.core.vm.ctrtasks.UsbDetach;
import ch.admin.vbs.cube.core.vm.list.VmDescriptor;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;

public class VmController implements IVmProductListener {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(VmController.class);
	private HashMap<VmModel, ModelListener> lIndex = new HashMap<VmModel, VmController.ModelListener>();
	private Object updateLock = new Object();
	private VBoxProduct product;
	private Stager stagger;
	private Map<String, VmStatus> tempStatus = new  HashMap<String, VmStatus>();
	private Executor exec = Executors.newCachedThreadPool();
	private IContainerFactory containerFactory;
	private VpnManager vpnManager;

	public VmController() {
		// eventually there is only one supported product: VirtualBox
		product = new VBoxProduct();
	}

	public void setTempStatus(Vm vm, VmStatus temp) {
		synchronized (tempStatus) {
			tempStatus.put(vm.getId(), temp);
		}
	}

	public VmStatus getTempStatus(Vm vm) {
		synchronized (tempStatus) {
			return tempStatus.get(vm.getId());
		}
	}
	
	public VmStatus clearTempStatus(Vm vm) {
		synchronized (tempStatus) {
			return tempStatus.remove(vm.getId());
		}
	}


	public void controlVm(final Vm vm, final VmModel model, VmCommand cmd, final IIdentityToken id, final IKeyring keyring, final Container transfer,
			IOption option) {
		LOG.debug("controlVm [{}]", cmd);
		switch (cmd) {
		case STAGE:
			stagger.startStaging(vm, model, id, keyring);
			refreshVmStatus(vm);
			break;
		case START:
			if (vm.getVmStatus() == VmStatus.STOPPED) {
				exec.execute(new Start(this, keyring, vm, containerFactory, vpnManager, product, transfer, model, option));
			} else {
				LOG.warn("Vm MUST be stopped to be started.");
			}
			break;
		case POWER_OFF:
			exec.execute(new PowerOff(this, keyring, vm, containerFactory, vpnManager, product, transfer, model, option));
			break;
		case SAVE:
			exec.execute(new Save(this, keyring, vm, containerFactory, vpnManager, product, transfer, model, option));
			break;
		case INSTALL_GUESTADDITIONS:
			exec.execute(new InstallGuestAdditions(vm));
			break;
		case DELETE:
			exec.execute(new Delete(containerFactory, vm, model));
			break;
		case ATTACH_USB:
			exec.execute(new UsbAttach(vm, product, option));
			break;
		case DETACH_USB:
			exec.execute(new UsbDetach(vm, product, option));
			break;
		case LIST_USB:
			try {
				product.listUsb(vm, (UsbDeviceEntryList) option);
			} catch (VmException e) {
				LOG.error("Failed to list usb devices", e);
			}
			LOG.info("XXXXXXXXXXXXX usb list : "+((UsbDeviceEntryList)option).size());
			break;
		default:
			LOG.warn("Command not implemented [{}]", cmd);
			break;
		}
	}

	public void start() {
		stagger = new Stager(this, product);
		vpnManager = new VpnManager();
		try {
			containerFactory = ContainerFactoryProvider.getFactory();
		} catch (Exception e) {
			LOG.error("Failed to init container factory [" + CubeCommonProperties.getProperty("keyring.containerFactoryImpl") + "]", e);
		}
		product.addListener(this);
		product.start();
	}

	public void registerVmModel(VmModel vmModel) {
		synchronized (lIndex) {
			ModelListener l = new ModelListener(vmModel);
			vmModel.addModelChangeListener(l);
			lIndex.put(vmModel, l);
		}
	}

	public void unregisterVmModel(VmModel vmModel) {
		synchronized (lIndex) {
			ModelListener l = lIndex.remove(vmModel);
			vmModel.removeModelChangeListener(l);
		}
	}

	private class ModelListener implements IVmModelChangeListener {
		private final VmModel src;

		public ModelListener(VmModel src) {
			this.src = src;
		}

		@Override
		public void listUpdated() {
			// update all VMs in model
			synchronized (updateLock) {
				for (Vm vm : src.getVmList()) {
					refreshVmStatus(vm);
				}
			}
		}

		@Override
		public void vmUpdated(Vm vm) {
			// update given VM
			synchronized (updateLock) {
				refreshVmStatus(vm);
			}
		}
	}

	/** update vm state due to a change in model. */
	public void refreshVmStatus(Vm vm) {
		try {
			// is vm staggable?
			VmDescriptor desc = vm.getDescriptor();
			// lazy initialize VM's containers references
			if (desc.getLocalCfg().getVmContainerUid() != null) {
				vm.setVmContainer(Container.initContainerObject(desc.getLocalCfg().getVmContainerUid()));
			}
			if (desc.getLocalCfg().getRuntimeContainerUid() != null) {
				vm.setRuntimeContainer(Container.initContainerObject(desc.getLocalCfg().getRuntimeContainerUid()));
			}
			// check if container exists on disk
			if (stagger.isStaging(vm.getId())) {
				vm.setVmStatus(VmStatus.STAGING);
			} else {
				if (vm.getVmContainer() != null && vm.getRuntimeContainer() != null && vm.getRuntimeContainer().exists() && vm.getVmContainer().exists()) {
					LOG.debug("already stagged");
					VmStatus tmp = tempStatus.get(vm.getId());
					if (tmp != null) {
						VmProductState pstate = product.getProductState(vm);
						if (pstate == VmProductState.ERROR) {
							// cancel temporary status
							tempStatus.remove(vm.getId());
							vm.setVmStatus(VmStatus.ERROR);
						} else {
							LOG.debug("Rely on temporary status [{}] instead of product state [{}]", tmp, pstate);
							// when starting or stopping, rely on the temporary
							// status that we set in controlVm() method.
							vm.setVmStatus(tmp);
						}
					} else {
						// container are present on the disk. ask product in
						// order to determine VM state
						VmProductState pstate = product.getProductState(vm);
						LOG.debug("Rely on product status [{}]", pstate);
						switch (pstate) {
						case STARTING:
							vm.setVmStatus(VmStatus.STARTING);
							break;
						case RUNNING:
							vm.setVmStatus(VmStatus.RUNNING);
							break;
						case STOPPED:
							vm.setVmStatus(VmStatus.STOPPED);
							break;
						case STOPPING:
							vm.setVmStatus(VmStatus.STOPPING);
							break;
						case ERROR:
							vm.setVmStatus(VmStatus.ERROR);
							break;
						case UNKNOWN:
						default:
							LOG.debug("Unsupported status [" + product.getProductState(vm) + "]");
							vm.setVmStatus(VmStatus.ERROR);
							break;
						}
					}
				} else {
					vm.setVmStatus(VmStatus.STAGABLE);
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to update VM state", e);
			vm.setVmStatus(VmStatus.ERROR);
		}
		// find model which contains this VM and notify changes
		VmModel model = null;
		synchronized (lIndex) {
			for (VmModel m : lIndex.keySet()) {
				vm = m.findByInstanceUid(vm.getId());
				if (vm != null) {
					model = m;
					break;
				}
			}
		}
		if (model != null) {
			model.fireVmStateUpdatedEvent(vm);
		}
	}

	public Vm findVmById(String id) {
		synchronized (lIndex) {
			for (VmModel m : lIndex.keySet()) {
				Vm vm = m.findByInstanceUid(id);
				if (vm != null) {
					return vm;
				}
			}
			return null;
		}
	}

	// ==========================================
	// IProductListener
	// ==========================================
	@Override
	public void vmStateChanged(String id, VmProductState oldState, VmProductState newState) {
		Vm vm = findVmById(id);
		if (vm == null) {
			LOG.warn("Product state changed (but VM not in models) [" + oldState + "]->[" + newState + "]   [" + id + "]");
			return;
		}
		LOG.debug("Product state changed [" + oldState + "]->[" + newState + "]   [" + id + "]");
		switch (newState) {
		case RUNNING:
			/*
			 * since, product.startVm is asynchronously (do not wait that the VM
			 * is started to return). We will be notified that the VM is
			 * effectively running through this event.
			 */
			tempStatus.remove(vm.getId());
			refreshVmStatus(vm);
			break;
		case STOPPED:
			/*
			 * since, product.stopVm is asynchronously (delay before VBoxCache
			 * see that the VM is STOPPED). We will be notified that the VM is
			 * effectively stopped through this event.
			 */
			VmStatus status = tempStatus.remove(vm.getId());
			if (status == VmStatus.STOPPING) {
				LOG.debug("'STOPPING' VM reached 'STOPPED' status.");
				// expected STOPPED status
				refreshVmStatus(vm);
			} else {
				LOG.error("Unexpected STOPPED status. Power off VM.");
				// unexpected STOPPED status (VM's OS shutdown). power off VM.
				controlVm(vm, null, VmCommand.POWER_OFF, null, null, null, null);
			}
			break;
		case UNKNOWN:
			if (oldState == VmProductState.RUNNING && !tempStatus.containsKey(vm.getId())) {
				// guest OS probably shutdown. trigger shutdown
				LOG.debug("Cleanup VM");
				tempStatus.put(vm.getId(), VmStatus.STOPPING);
				controlVm(vm, null, VmCommand.POWER_OFF, null, null, null, null);
			}
		default:
			break;
		}
	}
}
