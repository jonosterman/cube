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

import java.security.KeyStore.Builder;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.common.UuidGenerator;
import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.ContainerException;
import ch.admin.vbs.cube.common.container.ContainerFactoryProvider;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.common.keyring.impl.KeyringException;
import ch.admin.vbs.cube.core.I18nBundleProvider;
import ch.admin.vbs.cube.core.network.vpn.VpnManager;
import ch.admin.vbs.cube.core.vm.vbox.VBoxProduct;
import ch.admin.vbs.cube.core.webservice.WebServiceFactory;
import cube.cubemanager.services.InstanceConfigurationDTO;

public class Stager {
	private Executor exec = Executors.newCachedThreadPool();
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(Stager.class);
	private HashMap<String, StagingThead> sthread = new HashMap<String, Stager.StagingThead>();
	private VmController controller;
	private final VBoxProduct product;
	private IContainerFactory containerFactory;
	private VpnManager vpnManager;

	public Stager(VmController controller, VBoxProduct product) {
		this.controller = controller;
		this.product = product;
		vpnManager = new VpnManager();
		// initialize container factory
		try {
			containerFactory = ContainerFactoryProvider.getFactory();
		} catch (Exception e) {
			LOG.error("Failed to init container factory", e);
		}
	}

	public boolean isStaging(String vmId) {
		synchronized (sthread) {
			return sthread.containsKey(vmId);
		}
	}

	public void startStaging(Vm vm, VmModel model, IIdentityToken id, IKeyring keyring) {
		synchronized (sthread) {
			// check staggable
			if (!validateVmForStaging(vm)) {
				LOG.error("VM not ready to be stagged.");
				return;
			} else {
				LOG.info("Start staging..");
				StagingThead t = new StagingThead(vm, model, id, keyring);
				synchronized (sthread) {
					sthread.put(vm.getId(), t);
				}
				exec.execute(t);
			}
		}
	}

	private boolean validateVmForStaging(Vm vm) {
		return vm != null && vm.getVmStatus() == VmStatus.STAGABLE && !isStaging(vm.getId());
	}

	private class StagingThead implements Runnable {
		private final Vm vm;
		private final IIdentityToken id;
		private long t0 = 0;
		private final IKeyring keyring;
		private final VmModel model;

		public StagingThead(Vm vm, VmModel model, IIdentityToken id, IKeyring keyring) {
			this.vm = vm;
			this.model = model;
			this.id = id;
			this.keyring = keyring;
		}

		@Override
		public void run() {
			LOG.debug("StagingThead: run");
			controller.refreshVmStatus(vm);
			String vmContUuid = UuidGenerator.generate();
			String rtContUuid = UuidGenerator.generate();
			EncryptionKey rtKey = null;
			EncryptionKey vmKey = null;
			Container vmContainer = null;
			Container rtContainer = null;
			try {
				chrono("Stage VM");
				// create keys
				keyring.createKey(vmContUuid);
				keyring.createKey(rtContUuid);
				vmKey = keyring.getKey(vmContUuid);
				rtKey = keyring.getKey(rtContUuid);
				chrono("Instance's keys generated [" + vm.getId() + "]");
				// request VM creation on web service
				InstanceConfigurationDTO cfg = requestVmCreation(id.getBuilder());
				chrono("VM instance created by web service [" + vm.getId() + "]");
				// create containers
				vmContainer = Container.initContainerObject(vmContUuid);
				rtContainer = Container.initContainerObject(rtContUuid);
				createContainers(vmContainer, vmKey, rtContainer, rtKey, cfg);
				vm.setVmContainer(vmContainer);
				vm.setRuntimeContainer(rtContainer);
				chrono("VM containers created locally [" + vm.getId() + "]");
				// open VM container to store staged VM
				containerFactory.mountContainer(vmContainer, vmKey);
				containerFactory.mountContainer(rtContainer, rtKey);
				vmKey.shred();
				rtKey.shred();
				chrono("VM containers mounted [" + vm.getId() + "]");
				// download image(s)
				download(cfg, vm, vmContainer, rtContainer, id.getBuilder());
			} catch (Exception e) {
				LOG.error("Stagging [" + vm.getId() + "] failed.", e);
			}
			synchronized (sthread) {
				sthread.remove(vm.getId());
			}
			LOG.debug("StaggingThead: exit");
			controller.refreshVmStatus(vm);
		}

		private void download(InstanceConfigurationDTO cfg, Vm vm, Container vmContainer, Container rtContainer, Builder builder) throws VmException {
			// pre
			product.preStagging(vm);
			// staging
			vpnManager.stagging(vm, cfg, keyring);
			product.stagging(vm, model, cfg, builder);
			// post
			product.postStagging(vm);
			// Finalize
			LOG.debug("Unmount stagged VM containers");
			try {
				containerFactory.unmountContainer(vmContainer);
			} catch (Exception e2) {
				LOG.debug("Failed to unmount containers", e2);
			}
			try {
				containerFactory.unmountContainer(rtContainer);
			} catch (Exception e2) {
				LOG.debug("Failed to unmount containers", e2);
			}
			// update descriptor (local config) with new values
			vm.getDescriptor().getLocalCfg().setVmContainerUid(vmContainer.getId());
			vm.getDescriptor().getLocalCfg().setRuntimeContainerUid(rtContainer.getId());
			//
			vm.setProgressMessage("");
			// fire update events (to make the cache to be saved)
			model.fireVmUpdatedEvent(vm);
			// fire update event (to refresh UI)
			model.fireVmStateUpdatedEvent(vm);
		}

		private void createContainers(Container vmContainer, EncryptionKey vmKey, Container rtContainer, EncryptionKey rtKey, InstanceConfigurationDTO remoteCfg)
				throws KeyringException, ContainerException {
			// create vm container
			vmContainer.setSize(product.getPreferredVmDiskSize(remoteCfg));
			vm.setVmContainer(vmContainer);
			LOG.debug("Create VM container");
			if (vmContainer.getContainerFile().exists()) {
				vmContainer.getContainerFile().delete();
			}
			vmKey = keyring.getKey(vmContainer.getId());
			vm.setProgressMessage(I18nBundleProvider.getBundle().getString("staging.create_vm_container"));
			LOG.debug("container size (bytes) [" + vmContainer.getSize() + "] ");
			containerFactory.createContainer(vmContainer, vmKey);
			// create runtime container
			rtContainer.setSize(product.getPreferredRuntimeDiskSize(remoteCfg));
			vm.setRuntimeContainer(rtContainer);
			LOG.debug("Create snapshot container");
			if (rtContainer.getContainerFile().exists()) {
				rtContainer.getContainerFile().delete();
			}
			rtKey = keyring.getKey(rtContainer.getId());
			vm.setProgressMessage(I18nBundleProvider.getBundle().getString("staging.create_runtime_container"));
			containerFactory.createContainer(rtContainer, rtKey);
		}

		private void chrono(String message) {
			if (t0 == 0) {
				LOG.debug(message);
			} else {
				LOG.debug(message + " [{} ms]", System.currentTimeMillis() - t0);
			}
			t0 = System.currentTimeMillis();
		}

		private InstanceConfigurationDTO requestVmCreation(Builder builder) throws CubeException {
			WebServiceFactory wsFactory = new WebServiceFactory(builder);
			InstanceConfigurationDTO cfg = wsFactory.createCubeManagerService().getInstanceConfiguration(vm.getId());
			LOG.debug("Instance created [{}] on the server [{} ms]", vm.getId(), System.currentTimeMillis() - t0);
			return cfg;
		}
	}
}
