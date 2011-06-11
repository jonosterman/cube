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

package ch.admin.vbs.cube.core.impl;

import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.common.keyring.impl.KeyringProvider;
import ch.admin.vbs.cube.core.I18nBundleProvider;
import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISessionUI;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmController;
import ch.admin.vbs.cube.core.vm.VmModel;
import ch.admin.vbs.cube.core.vm.list.DescriptorModelCache;
import ch.admin.vbs.cube.core.vm.list.WSDescriptorUpdater;

/**
 * A session object is created for each user, once he is successfully logged in.
 * This object own its own thread used to open or close the session, since it is
 * slow (opening/closing encrypted containers). It should not block the caller
 * since it is the LoginMachin that MUST be free to react to user interactions
 * (insert/remove its token)
 * 
 * The stateCnt variable ensure we detect if state changes have been requested
 * between the beginning of the switch statement and its end. If there was one
 * or many changed, the last request will be proceeded (skipping all other
 * requests) in order to reach the last demanded state.
 */
public class Session implements Runnable, ISession {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(Session.class);
	private IIdentityToken id;
	private Thread thread;
	private int stateCnt = 0;

	//
	public enum State {
		open, close, lock, idle, error
	};

	private State state = State.idle;
	private long timestamp;
	private Object lock = new Object();
	private final ISessionUI sessionUI;
	private IKeyring keyring;
	private Container transfer;
	private IContainerFactory containerFactory;
	private TransferContainerFactory trfFactory = new TransferContainerFactory();
	private ResourceBundle bundle;
	private DescriptorModelCache descModelCache;
	private VmModel vmModel;
	private WSDescriptorUpdater descWs;
	private final VmController vmController;

	public Session(IIdentityToken id, ISessionUI clientUI, VmController vmController) {
		this.id = id;
		this.sessionUI = clientUI;
		this.vmController = vmController;
		vmModel = new VmModel();
		vmController.registerVmModel(vmModel);
		bundle = I18nBundleProvider.getBundle();
		thread = new Thread(this, "Session-(" + id.getSubjectName() + ")");
		thread.start();
	}

	@Override
	public VmModel getModel() {
		return vmModel;
	}

	@Override
	public IIdentityToken getId() {
		return id;
	}

	@Override
	public void setId(IIdentityToken id) {
		this.id = id;
		keyring.setId(id);
		// restart the web service since the keystore has been updated
		synchronized (vmModel) {
			if (descWs != null) {
				descWs.stop();
				descWs = new WSDescriptorUpdater(vmModel, id.getBuilder());
				descWs.start();
			}
		}
	}

	@Override
	public void run() {
		while (true) {
			timestamp = System.currentTimeMillis();
			int flag = stateCnt;
			LOG.debug("Proceed state [{}]", state);
			switch (state) {
			case idle:
			case error:
				synchronized (lock) {
					try {
						// it may have changed since switch statement (->
						// deadlock);
						if (state == State.idle || state == State.error)
							lock.wait();
					} catch (InterruptedException e) {
						LOG.error("Failure", e);
					}
				}
				break;
			case open:
				// Transfer Container
				sessionUI.showDialog(bundle.getString("login.open_transfer"), this);
				try {
					if (transfer == null) {
						LOG.debug("Initialize transfer container");
						transfer = trfFactory.initTransfer(id);
					} else {
						LOG.debug("Transfer container is already opened");
					}
				} catch (Exception e1) {
					LOG.error("Failed to open init transfer container", e1);
					sessionUI.showDialog(bundle.getString("login.error_transfer"), this);
					state = State.error;
					break;
				}
				// Keyring
				sessionUI.showDialog(bundle.getString("login.open_keyring"), this);
				try {
					if (keyring == null) {
						IKeyring kr = KeyringProvider.getInstance().getKeyring();
						kr.open(id, transfer.getMountpoint());
						keyring = kr;
						// setup cache to save descriptor in a file in keyring
						descModelCache = new DescriptorModelCache(vmModel, keyring.getFile("descriptors-cache"));
						descModelCache.start();
					}
				} catch (Exception e) {
					LOG.error("Failed to open keyring", e);
					sessionUI.showDialog(bundle.getString("login.error_keyring"), this);
					state = State.error;
					break;
				}
				sessionUI.closeDialog(this);
				// start web service
				try {
					if (descWs == null) {
						descWs = new WSDescriptorUpdater(vmModel, id.getBuilder());
						descWs.start();
					}
				} catch (Exception e) {
					LOG.error("Failed to setup descriptor cache", e);
					sessionUI.showDialog(bundle.getString("login.error_descriptor"), this);
					state = State.error;
					break;
				}
				// and wait
				synchronized (lock) {
					if (flag == stateCnt) {
						state = State.idle;
					}
				}
				break;
			case lock:
				// lock session
				// ...
				LOG.error("LOCKING NOT IMPLEMENTED");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// and wait
				synchronized (lock) {
					if (flag == stateCnt) {
						state = State.idle;
					}
				}
				break;
			case close:
				// standby all VMs
				// ..todo
				// stop web service
				if (descWs != null) {
					descWs.stop();
					descWs = null;
				}
				// unregister vm
				vmController.unregisterVmModel(vmModel);
				// close keyring
				try {
					keyring.close();
					keyring = null;
				} catch (Exception e) {
					LOG.error("Failed to close keyring", e);
				}
				// close transfer volume
				try {
					trfFactory.disposeTransfer(transfer);
					transfer = null;
				} catch (Exception e) {
					LOG.error("Failed to close keyring", e);
				}
				LOG.debug("Session close. Session's thread stopped.");
				// exit thread
				return;
			default:
				break;
			}
			LOG.debug("State completed in [{} sec]", String.format("%.3f", (System.currentTimeMillis() - timestamp) / 1000f));
		}
	}

	@Override
	public void open() {
		LOG.debug("Trigger 'open'");
		synchronized (lock) {
			state = State.open;
			stateCnt++;
			lock.notifyAll();
		}
	}

	@Override
	public void lock() {
		LOG.debug("Trigger 'lock'");
		synchronized (lock) {
			state = State.lock;
			stateCnt++;
			lock.notifyAll();
		}
	}

	@Override
	public void close() {
		LOG.debug("Trigger 'close'");
		synchronized (lock) {
			state = State.close;
			stateCnt++;
			lock.notifyAll();
		}
	}

	@Override
	public void setContainerFactory(IContainerFactory containerFactory) {
		this.containerFactory = containerFactory;
		trfFactory.setContainerFactory(this.containerFactory);
	}

	@Override
	public void controlVm(String vmId, VmCommand cmd, IOption option) {
		if (state == State.idle && keyring != null) {
			Vm vm = vmModel.findByInstanceUid(vmId);
			if (vm == null) {
				LOG.debug("VM not found [" + vmId + "] in this session.");
			} else {
				vmController.controlVm(vm, vmModel, cmd, id, keyring, transfer, option);
			}
		} else {
			LOG.debug("Ignore command [" + cmd + "] because session is not ready [" + state + " / " + keyring + "].");
		}
	}
}
