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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.core.I18nBundleProvider;
import ch.admin.vbs.cube.core.IClientFacade;
import ch.admin.vbs.cube.core.ICoreFacade;
import ch.admin.vbs.cube.core.ILoginUI;
import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISession.VmCommand;
import ch.admin.vbs.cube.core.ISessionManager;
import ch.admin.vbs.cube.core.ISessionManager.ISessionManagerListener;
import ch.admin.vbs.cube.core.ISessionUI;
import ch.admin.vbs.cube.core.usb.UsbDevice;
import ch.admin.vbs.cube.core.vm.IVmModelChangeListener;
import ch.admin.vbs.cube.core.vm.IVmStateChangeListener;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmStatus;

/**
 * CubeCore wrap around the SessionManager (which manages multi-sessions). It
 * maintains a reference on the current active session and make sure that only
 * this session may access the UI.
 * 
 * It react to VM events of the active session (model, state) and trigger UI
 * refresh.
 * 
 */
public class CubeCore implements ICoreFacade, ISessionUI, ILoginUI, ISessionManagerListener, IVmModelChangeListener, IVmStateChangeListener {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(CubeCore.class);
	private IClientFacade clientFacade;
	private ISessionManager smanager;
	private ISession actSession; // active session
	private ILoginUICallback currentCallback;
	private boolean loginMode;
	private Executor exec = Executors.newCachedThreadPool();

	/**
	 * Set current active session. Derergister-register event listener
	 */
	private void setActiveSession(ISession session) {
		if (actSession != null) {
			actSession.getModel().removeModelChangeListener(this);
			actSession.getModel().removeStateChangeListener(this);
		}
		actSession = session;
		if (session != null) {
			session.getModel().addModelChangeListener(this);
			session.getModel().addStateChangeListener(this);
		}
	}

	// ==============================================
	// IVmStatelListener (of active session only)
	// ==============================================
	@Override
	public void vmStateUpdated(Vm vm) {
		synchronized (clientFacade) {
			refreshVmUI(vm);
		}
	}

	private void refreshVmUI(Vm vm) {
		synchronized (clientFacade) {
			// actSession shall not be null (but let test it anyway) and also
			// check that the given vm is referenced in the active session's
			// model.
			if (actSession != null && actSession.getModel().findByInstanceUid(vm.getId()) != null) {
				LOG.debug("REFRESH UI [1 vm: " + vm.getDescriptor().getRemoteCfg().getName() + " / " + vm.getVmStatus() + "] (but refresh all tabs)");
				// LOG.debug("REFRESH UI [1 vm: " +
				// vm.getDescriptor().getRemoteCfg().getName() + "]");
				clientFacade.notifiyVmUpdated(vm);
			}
		}
	}

	// ==============================================
	// IVmModelListener (of active session only)
	// ==============================================
	public void listUpdated() {
		displayVmsOfActiveSession();
	}

	@Override
	public void vmUpdated(Vm vm) {
		refreshVmUI(vm);
	}

	private void displayVmsOfActiveSession() {
		LOG.debug("Update VM list for active session [" + actSession + "]");
		synchronized (clientFacade) {
			// actSession shall not be null... but let test it anyway.
			if (actSession != null) {
				List<Vm> vms = actSession.getModel().getVmList();
				// System.out.println("REFRESH UI (displayVmsOfActiveSession) ["+vms.size()+" vms]");
				clientFacade.displayTabs(vms);
			} else {
				LOG.debug("No active session (null). Do not refresh VM list");
			}
		}
	}

	// ==============================================
	// ICoreFacade
	// ==============================================
	@Override
	public void enteredPassword(char[] password) {
		ILoginUICallback cc = currentCallback;
		synchronized (clientFacade) {
			if (currentCallback != null) {
				cc = currentCallback;
				currentCallback = null;
			}
		}
		if (cc != null) {
			if (password == null || password.length == 0) {
				cc.abort();
			} else {
				cc.passwordEntered(password);
			}
		}
	}

	private void controlVm(String vmId, VmCommand cmd) {
		// thread safe not-null test
		ISession ses = actSession;
		if (ses != null) {
			ses.getModel().findByInstanceUid(vmId);
			if (cmd == VmCommand.DELETE) {
				if (clientFacade.askConfirmation("messagedialog.confirmation.deleteVmConfirmation") != 1) {
					LOG.debug("User aborted delete action.");
					return;
				}
			}
			ses.controlVm(vmId, cmd);
		}
	}

	@Override
	public void startVm(String vmId) {
		controlVm(vmId, VmCommand.START);
	}

	@Override
	public void standByVm(String vmId) {
		controlVm(vmId, VmCommand.SAVE);
	}

	@Override
	public void powerOffVm(String vmId) {
		controlVm(vmId, VmCommand.POWER_OFF);
	}

	@Override
	public void stageVm(String vmId, URL location) {
		controlVm(vmId, VmCommand.STAGE);
	}

	@Override
	public void logoutUser() {
		ISession ses = actSession;
		if (clientFacade.askConfirmation("messagedialog.confirmation.closeSessionConfirmation") != 1) {
			LOG.debug("User aborted shutdown.");
			return;
		}
		// save all VMs
		closeAllSessionVM(ses);
		// close session
		smanager.closeSession(ses);
	}

	@Override
	public void fileTransfer(RelativeFile fileName, String vmIdFrom, String vmIdTo) {
		LOG.error("FileTransfer not implemented");
	}

	@Override
	public void cleanUpExportFolder(String vmId) {
		LOG.error("cleanup not implemented");
	}

	@Override
	public void deleteVm(String vmId) {
		controlVm(vmId, VmCommand.DELETE);
	}

	@Override
	public void installGuestAdditions(String vmId) {
		controlVm(vmId, VmCommand.INSTALL_GUESTADDITIONS);
	}

	@Override
	public void lockCube() {
		LOG.error("lockCube not implemented");
	}

	private void closeAllSessionVM(final ISession session) {
		// Index running VMs
		ArrayList<Vm> vmIndex = new ArrayList<Vm>();
		// Start 'SAVE' for all running VMs
		for (final Vm vm : session.getModel().getVmList()) {
			if (vm.getVmStatus() == VmStatus.RUNNING) {
				vmIndex.add(vm);
				exec.execute(new Runnable() {
					@Override
					public void run() {
						LOG.debug("Save VM [{}]", vm.getDescriptor().getRemoteCfg().getName());
						session.controlVm(vm.getId(), VmCommand.SAVE);
					}
				});
			}
		}
		// Wait that all VMs have been saved
		long timeout = System.currentTimeMillis() + 30000;
		int remindBkp = -1;
		WAITLOOP: while (System.currentTimeMillis() < timeout) {
			int remind = 0;
			for (Vm vm : vmIndex) {
				if (vm.getVmStatus() == VmStatus.STOPPING || vm.getVmStatus() == VmStatus.RUNNING) {
					LOG.debug("Wait on VM [{}][{}]", vm.getDescriptor().getRemoteCfg().getName(), vm.getVmStatus());
					remind++;
				}
			}
			if (remind == 0) {
				LOG.debug("All VMs [{}] are stopped [{} secs]", vmIndex.size(), (timeout - System.currentTimeMillis()) / 1000);
				break WAITLOOP;
			} else {
				if (remindBkp != remind) {
					remindBkp = remind;
					clientFacade.showMessage(I18nBundleProvider.getBundle().getString("login.waitstopped") + " (" + remind + " VMs)",
							IClientFacade.OPTION_NONE);
				}
				LOG.debug("Wait on [{}] VMs", remind);
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
			}
		}
	}

	@Override
	public void shutdownMachine() {
		if (clientFacade.askConfirmation("messagedialog.confirmation.shutdownCubeConfirmation") != 1) {
			LOG.debug("User aborted shutdown.");
			return;
		}
		// lock
		setActiveSession(null);
		clientFacade.showMessage("cube.shutdown", IClientFacade.OPTION_NONE);
		// try to save all running VMs of all users
		List<ISession> sessions = smanager.getSessions();
		List<Vm> vmToWait = new ArrayList<Vm>();
		for (final ISession s : sessions) {
			for (final Vm vm : s.getModel().getVmList()) {
				if (vm.getVmStatus() == VmStatus.RUNNING) {
					vmToWait.add(vm);
					exec.execute(new Runnable() {
						@Override
						public void run() {
							s.controlVm(vm.getId(), VmCommand.SAVE);
						}
					});
				}
			}
		}
		// wait all VM to be saved (max 1 minute)
		long timeout = System.currentTimeMillis() + 60000;
		int remindBkp = -1;
		while (System.currentTimeMillis() < timeout) {
			int remind = 0;
			for (Vm vm : vmToWait) {
				if (vm.getVmStatus() == VmStatus.STOPPING || vm.getVmStatus() == VmStatus.RUNNING) {
					LOG.debug("Wait on VM [{}][{}]", vm.getDescriptor().getRemoteCfg().getName(), vm.getVmStatus());
					remind++;
				}
			}
			if (remind == 0) {
				LOG.debug("All VMs [{}] are stopped", vmToWait.size());
				break;
			} else {
				if (remindBkp != remind) {
					remindBkp = remind;
					clientFacade.showMessage(I18nBundleProvider.getBundle().getString("login.waitstopped") + " (" + remind + ")",
							IClientFacade.OPTION_NONE);
				}
				LOG.debug("Wait on [{}] VMs", remind);
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
			}
		}
		// logout all users
		for (ISession s : sessions) {
			smanager.closeSession(s);
		}
		// shudown machine
		clientFacade.showMessage(I18nBundleProvider.getBundle().getString("login.shutdown"), IClientFacade.OPTION_NONE);
		ScriptUtil su = new ScriptUtil();
		try {
			su.execute("sudo", "./cube-shutdown.pl");
		} catch (Exception e) {
			LOG.error("Failed to shutdown computer", e);
		}
	}

	@Override
	public void connectUsbDevice(String vmId, UsbDevice usbDevice) {
		LOG.error("connect usb not implemented");
	}

	public void setup(IClientFacade clientFacade, ISessionManager smanager) {
		this.clientFacade = clientFacade;
		this.smanager = smanager;
		//
		smanager.addListener(this);
	}

	// ==============================================
	// ILoginUI
	// ==============================================
	@Override
	public void closeDialog() {
		loginMode = false;
		synchronized (clientFacade) {
			clientFacade.closeDialog();
			displayVmsOfActiveSession();
		}
	}

	@Override
	public void showDialog(String message, LoginDialogType type) {
		loginMode = true;
		synchronized (clientFacade) {
			switch (type) {
			case NOOPTION:
				clientFacade.showMessage(message, IClientFacade.OPTION_NONE);
				break;
			case SHUTDOW_OPTION:
				clientFacade.showMessage(message, IClientFacade.OPTION_SHUTDOWN);
				break;
			default:
				LOG.error("Bad option [" + type + "]");
				break;
			}
		}
	}

	@Override
	public void showPinDialog(String message, ILoginUICallback callback) {
		synchronized (clientFacade) {
			loginMode = true;
			if (currentCallback != null) {
				LOG.debug("Abort previous callback");
				currentCallback.abort();
			}
			currentCallback = callback;
			clientFacade.showGetPIN(message);
		}
	}

	// ==============================================
	// ISessionUI
	// ==============================================
	@Override
	public void closeDialog(ISession session) {
		if (!loginMode && session == actSession) {
			// only allow session to do this, if login do not use the UI.
			LOG.warn("closeDialog (session). display tabs");
			synchronized (clientFacade) {
				clientFacade.closeDialog();
				displayVmsOfActiveSession();
			}
		}
	}

	@Override
	public void showDialog(String message, ISession session) {
		if (!loginMode && session == actSession) {
			// only allow session to do this, if
			// login do not use the UI.
			synchronized (clientFacade) {
				clientFacade.showMessage(message, IClientFacade.OPTION_NONE);
			}
		}
	}

	@Override
	public void showWorkspace(ISession session) {
		if (!loginMode && session == actSession) {
			synchronized (clientFacade) {
				displayVmsOfActiveSession();
			}
		}
	}

	// ==============================================
	// ISessionManagerListener
	// ==============================================
	@Override
	public void sessionOpened(ISession session) {
		synchronized (clientFacade) {
			if (actSession != null && actSession == session) {
				// lock active session
				// @TODO
			}
			// set the new opened session the active one
			setActiveSession(session);
		}
	}

	@Override
	public void sessionClosed(ISession session) {
		synchronized (clientFacade) {
			if (actSession != null && actSession == session) {
				setActiveSession(null);
			}
		}
	}

	@Override
	public void sessionUpdated(ISession session) {
		// @TODO
	}
}
