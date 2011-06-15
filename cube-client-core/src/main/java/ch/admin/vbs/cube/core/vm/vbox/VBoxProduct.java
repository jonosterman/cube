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
package ch.admin.vbs.cube.core.vm.vbox;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore.Builder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.virtualbox_4_0.AccessMode;
import org.virtualbox_4_0.AudioControllerType;
import org.virtualbox_4_0.AudioDriverType;
import org.virtualbox_4_0.BIOSBootMenuMode;
import org.virtualbox_4_0.CPUPropertyType;
import org.virtualbox_4_0.CleanupMode;
import org.virtualbox_4_0.ClipboardMode;
import org.virtualbox_4_0.DeviceType;
import org.virtualbox_4_0.HWVirtExPropertyType;
import org.virtualbox_4_0.IConsole;
import org.virtualbox_4_0.IHostUSBDevice;
import org.virtualbox_4_0.IMachine;
import org.virtualbox_4_0.IMedium;
import org.virtualbox_4_0.IMediumAttachment;
import org.virtualbox_4_0.INetworkAdapter;
import org.virtualbox_4_0.IProgress;
import org.virtualbox_4_0.ISession;
import org.virtualbox_4_0.IStorageController;
import org.virtualbox_4_0.IUSBDevice;
import org.virtualbox_4_0.IVirtualBox;
import org.virtualbox_4_0.LockType;
import org.virtualbox_4_0.MachineState;
import org.virtualbox_4_0.SessionState;
import org.virtualbox_4_0.StorageBus;
import org.virtualbox_4_0.StorageControllerType;
import org.virtualbox_4_0.VBoxException;
import org.virtualbox_4_0.VirtualBoxManager;

import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.common.container.SizeFormatUtil;
import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;
import ch.admin.vbs.cube.core.I18nBundleProvider;
import ch.admin.vbs.cube.core.usb.UsbDevice;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntry;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntry.DeviceEntryState;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntryList;
import ch.admin.vbs.cube.core.vm.IVmProduct.VmProductState;
import ch.admin.vbs.cube.core.vm.IVmProductListener;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmException;
import ch.admin.vbs.cube.core.vm.VmModel;
import ch.admin.vbs.cube.core.vm.vbox.VBoxCache.VBoxCacheListener;
import ch.admin.vbs.cube.core.vm.vbox.VBoxConfig.VBoxOption;
import ch.admin.vbs.cube.core.webservice.FileDownloader;
import ch.admin.vbs.cube.core.webservice.FileDownloader.State;
import ch.admin.vbs.cube.core.webservice.InstanceParameterHelper;
import cube.cubemanager.services.InstanceConfigurationDTO;

public class VBoxProduct implements VBoxCacheListener {
	private static final int API_RETRY = 5;
	private static final String CONTROLLER_NAME = "IDE Controller";
	public static final String SNAPSHOT_DIRECTORY = "snapshots";
	private static final String DISK1 = "disk1";
	private static final long MEGA = 1048576;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(VBoxProduct.class);
	//
	private VirtualBoxManager mgr;
	private IVirtualBox vbox;
	private Object wsLock = new Object();
	private VBoxCache cache;
	private ArrayList<IVmProductListener> listeners = new ArrayList<IVmProductListener>(2);
	private boolean connected = false;

	public VBoxProduct() {
		cache = new VBoxCache(this);
	}

	public void start() {
		// disable CXF logs. way too chatty and use "java.logging".
		java.util.logging.Logger.getLogger("org.apache.cxf").setLevel(Level.SEVERE);
		// start cache / monitor
		cache.start();
		// Connect VirtualBox Web Service in a new thread to avoid
		// blocking login for 5 seconds.
		reconnect();
	}

	/** used by VBoxCache */
	public List<IMachine> getMachines() {
		if (!connected)
			return new ArrayList<IMachine>(0);
		synchronized (wsLock) {
			return vbox.getMachines();
		}
	}

	// ==========================================
	// VBoxCacheListener
	// ==========================================
	@Override
	public void notifyVmAdded(IMachine m) {
		for (IVmProductListener l : listeners) {
			l.vmStateChanged(m.getId(), VmProductState.STOPPED, getProductState(m));
		}
	}

	@Override
	public void notifyVmRemoved(IMachine m) {
		for (IVmProductListener l : listeners) {
			l.vmStateChanged(m.getId(), getProductState(m), VmProductState.STOPPED);
		}
	}

	@Override
	public void notifyVmStateChanged(IMachine machine, MachineState oldState) {
		for (IVmProductListener l : listeners) {
			l.vmStateChanged(machine.getId(), getProductState(oldState), getProductState(machine));
		}
	}

	private VmProductState getProductState(IMachine m) {
		if (m == null) {
			return VmProductState.STOPPED;
		} else {
			return getProductState(m.getState());
		}
	}

	private VmProductState getProductState(MachineState m) {
		if (m == null) {
			return VmProductState.STOPPED;
		}
		//
		switch (m) {
		case Running:
		case Paused:
			return VmProductState.RUNNING;
		case Restoring:
		case RestoringSnapshot:
		case Starting:
			return VmProductState.STARTING;
		case Saving:
		case Stopping:
			return VmProductState.STOPPING;
		case Saved:
		case PoweredOff:
			// these states could occurs during starting and stopping ...
			// and it is problematic. We solved it in VmController.
			return VmProductState.UNKNOWN;
		case Aborted:
			return VmProductState.ERROR;
		default:
			LOG.debug("The virtual box status '" + m + "' is not interpreted!");
			return VmProductState.UNKNOWN;
		}
	}

	public void addListener(IVmProductListener l) {
		listeners.add(l);
	}

	public long getPreferredVmDiskSize(InstanceConfigurationDTO config) {
		long dis1Size = Long.parseLong(InstanceParameterHelper.getInstanceParameter("vbox.disk1Size", config));
		long cntSize = (long) ((dis1Size * 1.2) + (200 * MEGA));
		LOG.debug("compute container size [{}] to hold file [{}]", SizeFormatUtil.format(cntSize), SizeFormatUtil.format(dis1Size));
		return cntSize;
	}

	public long getPreferredRuntimeDiskSize(InstanceConfigurationDTO config) {
		// vbox.baseMemory is received in MEGA unit not bytes
		long baseMemory = Long.parseLong(InstanceParameterHelper.getInstanceParameter("vbox.baseMemory", config)) * MEGA;
		// reserve twice its size
		long cntSize = baseMemory * 2;
		LOG.debug("compute container size [{}] to hold file [{}]", SizeFormatUtil.format(cntSize), SizeFormatUtil.format(baseMemory));
		return cntSize;
	}

	public void preStagging(Vm vm) {
		// not used with virtualbox
	}

	public void stagging(Vm vm, VmModel model, InstanceConfigurationDTO config, Builder builder) throws VmException {
		try {
			LOG.debug("Start VirtualBox Image Stagging [{}]", vm.getId());
			vm.setProgressMessage(I18nBundleProvider.getBundle().getString("staging.vbox.download_image"));
			model.fireVmStateUpdatedEvent(vm);
			// write config
			VBoxConfig cfg = new VBoxConfig(vm.getVmContainer(), vm.getRuntimeContainer());
			cfg.load();
			cfg.setOption(VBoxOption.Disk1File, DISK1);
			cfg.setOption(VBoxOption.OsType, InstanceParameterHelper.getInstanceParameter("vbox.operatingSystem", config));
			cfg.setOption(VBoxOption.IoApic, InstanceParameterHelper.getInstanceParameter("vbox.ioApic", config));
			cfg.setOption(VBoxOption.Acpi, InstanceParameterHelper.getInstanceParameter("vbox.acpi", config));
			cfg.setOption(VBoxOption.Pae, InstanceParameterHelper.getInstanceParameter("vbox.paenx", config));
			cfg.setOption(VBoxOption.BaseMemory, InstanceParameterHelper.getInstanceParameter("vbox.baseMemory", config));
			cfg.setOption(VBoxOption.VideoMemory, InstanceParameterHelper.getInstanceParameter("vbox.videoMemory", config));
			cfg.setOption(VBoxOption.Audio, "on");
			// network
			cfg.setOption(VBoxOption.Nic1, InstanceParameterHelper.getInstanceParameter("vbox.nic1", config));
			cfg.setOption(VBoxOption.Nic2, InstanceParameterHelper.getInstanceParameter("vbox.nic2", config));
			cfg.setOption(VBoxOption.Nic3, InstanceParameterHelper.getInstanceParameter("vbox.nic3", config));
			cfg.setOption(VBoxOption.Nic4, InstanceParameterHelper.getInstanceParameter("vbox.nic4", config));
			cfg.setOption(VBoxOption.Nic1Mac, InstanceParameterHelper.getInstanceParameter("vbox.nic1Mac", config));
			cfg.setOption(VBoxOption.Nic2Mac, InstanceParameterHelper.getInstanceParameter("vbox.nic2Mac", config));
			cfg.setOption(VBoxOption.Nic3Mac, InstanceParameterHelper.getInstanceParameter("vbox.nic3Mac", config));
			cfg.setOption(VBoxOption.Nic4Mac, InstanceParameterHelper.getInstanceParameter("vbox.nic4Mac", config));
			cfg.setOption(VBoxOption.Nic1Bridge, InstanceParameterHelper.getInstanceParameter("vbox.nic1Bridge", config));
			cfg.setOption(VBoxOption.Nic2Bridge, InstanceParameterHelper.getInstanceParameter("vbox.nic2Bridge", config));
			cfg.setOption(VBoxOption.Nic3Bridge, InstanceParameterHelper.getInstanceParameter("vbox.nic3Bridge", config));
			cfg.setOption(VBoxOption.Nic4Bridge, InstanceParameterHelper.getInstanceParameter("vbox.nic4Bridge", config));
			cfg.save();
			// download VM from server via WebService
			FileDownloader down = new FileDownloader(builder);
			File tempFile = new File(vm.getVmContainer().getMountpoint(), DISK1 + ".tmp");
			FileOutputStream fos = new FileOutputStream(tempFile);
			down.setDestination(fos);
			long size = InstanceParameterHelper.getInstanceParameterAsLong("vbox.disk1Size", config);
			down.setRequest(config.getUuid(), size);
			down.startDownload();
			LOG.debug("Download started [{}]", SizeFormatUtil.format(size));
			while (down.getState() == State.DOWNLOADING || down.getState() == State.IDLE) {
				Thread.sleep(2000);
				vm.setProgress((int) (down.getProgress() * 100));
				model.fireVmStateUpdatedEvent(vm);
			}
			fos.close();
			if (down.getState() == State.SUCCESS) {
				tempFile.renameTo(new File(vm.getVmContainer().getMountpoint(), DISK1));
			} else {
				tempFile.delete();
			}
		} catch (Exception e) {
			throw new VmException("Failed to stage VirtualBox VM.", e);
		}
	}

	public void postStagging(Vm vm) throws VmException {
		ShellUtil shell = new ShellUtil();
		try {
			// update disk UUID
			shell.run(null, ShellUtil.NO_TIMEOUT, "VBoxManage", "-nologo", "internalcommands", "sethduuid",
					new File(vm.getVmContainer().getMountpoint(), DISK1).getAbsolutePath());
		} catch (ShellUtilException e) {
			throw new VmException("Failed to set HD uuid.", e);
		}
	}

	public void startVm(Vm vm) throws VmException {
		try {
			LOG.debug("Start VM [{}]", vm.getDescriptor().getRemoteCfg().getName());
			// start vm using VBoxSDL in order to be
			// able to contains the VM process in
			// another SELinux Context and Category
			ScriptUtil script = new ScriptUtil();
			script.execute( //
					"./vbox-startvm.pl", //
					"--uuid", //
					vm.getId(), //
					"--snapshot", //
					vm.getRuntimeContainer().getMountpoint().getAbsolutePath() //
			);
			// wait started (not more PoweredOff)
			IMachine imachine = getIMachineReference(vm.getId());
			for (int i = 0; i < 100; i++) {
				MachineState state = imachine.getState();
				if (state != MachineState.PoweredOff) {
					return;
				}
				Thread.sleep(100);
			}
		} catch (Exception e) {
			throw new VmException("Failed to start VM", e);
		}
	}

	public void registerVm(Vm vm) throws VmException {
		synchronized (wsLock) { // load configuration from VM container
			try {
				VBoxConfig cfg = new VBoxConfig(vm.getVmContainer(), vm.getRuntimeContainer());
				cfg.load();
				// check if VM is already registered. Clean it up if necessary.
				IMachine machine = getIMachineReference(vm.getId());
				if (machine != null) {
					LOG.debug("An older VM with the UID [{}] is already registred. Cleanup this VM before registering the new one.", vm.getId());
					ISession session = mgr.getSessionObject();
					// power-off in necessary
					machine = getIMachineReference(vm.getId());
					if (machine.getState() == MachineState.Running) {
						machine.lockMachine(session, LockType.Shared);
						machine = session.getMachine();
						// power off machine
						IProgress progress = session.getConsole().powerDown();
						// wait until completed
						while (!progress.getCompleted()) {
							// notify progress to model listeners
							LOG.debug("power-off progress [{}%]", progress.getOperationPercent());
							vm.setProgress(progress.getOperationPercent().intValue());
							Thread.sleep(500);
						}
						unlockSession(session);
					}
					// discard state if necessary
					machine = getIMachineReference(vm.getId());
					if (machine.getState() == MachineState.Saved) {
						machine.lockMachine(session, LockType.Shared);
						machine = session.getMachine();
						session.getConsole().discardSavedState(true);
						unlockSession(session);
					}
					// remove HDD if necessary
					machine = getIMachineReference(vm.getId());
					for (IMediumAttachment atta : machine.getMediumAttachments()) {
						if (atta.getMedium() != null) {
							machine.lockMachine(session, LockType.Shared);
							machine = session.getMachine();
							LOG.debug("Remove medium [{}]", atta.getMedium().getName());
							machine.detachDevice(atta.getController(), atta.getPort(), atta.getDevice());
							machine.saveSettings();
							atta.getMedium().close();
						}
					}
					unlockSession(session);
					//
					machine = getIMachineReference(vm.getId());
					machine.unregister(CleanupMode.Full);
				}
				// register VM (using web service)
				LOG.debug("Register VM [{}].", vm.getId());
				machine = vbox.createMachine(null, vm.getId(), cfg.getOption(VBoxOption.OsType), vm.getId(), true);
				// configure VM
				LOG.debug("Configure VM [{}].", vm.getId());
				machine.getBIOSSettings().setIOAPICEnabled(cfg.getOptionAsBoolean(VBoxOption.IoApic));
				machine.getBIOSSettings().setACPIEnabled(cfg.getOptionAsBoolean(VBoxOption.Acpi));
				machine.getBIOSSettings().setBootMenuMode(BIOSBootMenuMode.MenuOnly);
				machine.getBIOSSettings().setLogoDisplayTime(0l);
				machine.setHWVirtExProperty(HWVirtExPropertyType.NestedPaging, cfg.getOptionAsBoolean(VBoxOption.Pae));
				machine.setMemorySize(cfg.getOptionAsLong(VBoxOption.BaseMemory));
				machine.setVRAMSize(cfg.getOptionAsLong(VBoxOption.VideoMemory));
				machine.setSnapshotFolder(vm.getRuntimeContainer().getMountpoint().getAbsolutePath());
				machine.createSharedFolder("export", vm.getExportFolder().getAbsolutePath(), true, true);
				machine.createSharedFolder("import", vm.getImportFolder().getAbsolutePath(), false, true);
				machine.setAccelerate2DVideoEnabled(false);
				machine.setAccelerate3DEnabled(true);
				machine.setClipboardMode(ClipboardMode.Bidirectional);
				machine.setCPUCount(1l);
				machine.setCPUProperty(CPUPropertyType.PAE, true);
				machine.getUSBController().setEnabled(true);
				// configure sound card
				machine.getAudioAdapter().setAudioController(AudioControllerType.AC97);
				machine.getAudioAdapter().setAudioDriver(AudioDriverType.Pulse);
				machine.getAudioAdapter().setEnabled(true);
				// configure network interfaces
				addNetworkIface(0, cfg.getOption(VBoxOption.Nic1), cfg.getOption(VBoxOption.Nic1Bridge), null, machine);
				addNetworkIface(1, cfg.getOption(VBoxOption.Nic2), cfg.getOption(VBoxOption.Nic2Bridge), null, machine);
				addNetworkIface(2, cfg.getOption(VBoxOption.Nic3), cfg.getOption(VBoxOption.Nic3Bridge), null, machine);
				addNetworkIface(3, cfg.getOption(VBoxOption.Nic4), cfg.getOption(VBoxOption.Nic4Bridge), null, machine);
				LOG.debug("Save VM settings [{}].", vm.getId());
				synchronized (wsLock) {
					machine.saveSettings();
					vbox.registerMachine(machine);
				}
				// configure disks (need to lock the machine)
				Runtime.getRuntime()
						.exec("dd count=1000 if=/dev/zero of=/tmp/mp_vmctn/disk1.raw;rm -f /tmp/mp_vmctn/disk1;VBoxManage internalcommands converthd -srcformat RAW -dstformat VDI /tmp/mp_vmctn/disk1.raw /tmp/mp_vmctn/disk1");
				synchronized (wsLock) {
					ISession session = mgr.getSessionObject();
					machine.lockMachine(session, LockType.Write);
					machine = session.getMachine();
					try {
						//
						IStorageController store = machine.addStorageController(CONTROLLER_NAME, StorageBus.IDE);
						store.setControllerType(StorageControllerType.PIIX4);
						IMedium medium = vbox.openMedium(new File(vm.getVmContainer().getMountpoint(), cfg.getOption(VBoxOption.Disk1File)).getAbsolutePath(),
								DeviceType.HardDisk, AccessMode.ReadWrite);
						machine.attachDevice(CONTROLLER_NAME, 0, 0, DeviceType.HardDisk, medium);
						machine.attachDevice(CONTROLLER_NAME, 1, 0, DeviceType.DVD, null);
						machine.saveSettings();
					} catch (Exception e) {
						throw new CubeException("Fail to attach VM devices", e);
					} finally {
						unlockSession(session);
					}
				}
				// Create 'tun' devices (need sudo rights)
				ArrayList<String> nics = new ArrayList<String>();
				if ("vpn".equalsIgnoreCase(cfg.getOption(VBoxOption.Nic1))) {
					nics.add(cfg.getOption(VBoxOption.Nic1Bridge));
				}
				if ("vpn".equalsIgnoreCase(cfg.getOption(VBoxOption.Nic2))) {
					nics.add(cfg.getOption(VBoxOption.Nic2Bridge));
				}
				if ("vpn".equalsIgnoreCase(cfg.getOption(VBoxOption.Nic3))) {
					nics.add(cfg.getOption(VBoxOption.Nic3Bridge));
				}
				if ("vpn".equalsIgnoreCase(cfg.getOption(VBoxOption.Nic4))) {
					nics.add(cfg.getOption(VBoxOption.Nic4Bridge));
				}
				ScriptUtil script = new ScriptUtil();
				for (String nic : nics) {
					script.execute("sudo", "./vbox-tuncreate.pl", "--tun", nic);
				}
			} catch (Exception e) {
				throw new VmException("Failed to register VM", e);
			}
		}
	}

	private void addNetworkIface(long id, String nic, String bridge, String mac, IMachine machine) {
		LOG.debug("configure nic [{}]", id + " / " + nic);
		if ("vpn".equals(nic)) {
			// use pre-configured mac + bridge
			INetworkAdapter na = machine.getNetworkAdapter(id);
			na.attachToBridgedInterface();
			na.setHostInterface(bridge);
			na.setMACAddress(mac);
			na.setEnabled(true);
		} else if ("disabled".equals(nic)) {
			// Do not use it
			INetworkAdapter na = machine.getNetworkAdapter(id);
			na.detach();
			na.setEnabled(false);
		} else if ("bridged".equals(nic)) {
			// use pre-configured mac + bridge
			INetworkAdapter na = machine.getNetworkAdapter(id);
			na.attachToBridgedInterface();
			na.setHostInterface(bridge);
			na.setMACAddress(mac);
			na.setEnabled(true);
		} else if ("disconnected".equals(nic)) {
			// use pre-configured mac
			INetworkAdapter na = machine.getNetworkAdapter(id);
			na.detach();
			na.setMACAddress(mac);
			na.setEnabled(true);
		} else {
			LOG.error("unsupported nic [{}]", nic);
		}
	}

	/**
	 * Find machine by name. Return null instead an exception if no
	 * corresponding VM is found.
	 * 
	 * @param id
	 * @return
	 */
	private IMachine getIMachineReference(String id) {
		try {
			synchronized (wsLock) {
				if (vbox == null || id == null)
					return null;
				return vbox.findMachine(id);
			}
		} catch (VBoxException e) {
			return null;
		}
	}

	public void poweroffVm(Vm vm, VmModel model) throws VmException {
		synchronized (wsLock) {
			try {
				// get IMachine reference
				IMachine machine = getIMachineReference(vm.getId());
				if (machine.getState() == MachineState.PoweredOff) {
					// already power-off
					return;
				}
				ISession session = mgr.getSessionObject();
				// lock IMachine
				machine.lockMachine(session, LockType.Shared);
				machine = session.getMachine();
				try {
					// power off machine
					IProgress progress = session.getConsole().powerDown();
					// wait until completed
					while (!progress.getCompleted()) {
						// notify progress to model listeners
						LOG.debug("power-off progress [{}%]", progress.getOperationPercent());
						vm.setProgress(progress.getOperationPercent().intValue());
						model.fireVmStateUpdatedEvent(vm);
						Thread.sleep(500);
					}
				} finally {
					// ensure that session is unlocked
					unlockSession(session);
				}
				vm.setProgress(100);
				model.fireVmStateUpdatedEvent(vm);
			} catch (Exception e) {
				throw new VmException("", e);
			}
		}
	}

	private void unlockSession(ISession session) {
		synchronized (wsLock) {
			// it is not clear how/when to unlock session...
			if (session.getState() == SessionState.Locked) {
				try {
					session.unlockMachine();
					LOG.debug("Session unlocked");
				} catch (Exception e) {
					LOG.error("Failed to unlock session", e);
				}
			} else {
				LOG.error("Session was not locked [" + session.getState() + "]");
			}
		}
	}

	public void unregisterVm(Vm vm) throws VmException {
		synchronized (wsLock) {
			try {
				IMachine machine = getIMachineReference(vm.getId());
				if (machine != null) {
					// unregister all disks before unregistering VM
					ISession session = mgr.getSessionObject();
					machine.lockMachine(session, LockType.Write);
					machine = session.getMachine();
					for (IMediumAttachment atta : machine.getMediumAttachments()) {
						if (atta.getMedium() != null) {
							LOG.debug("Remove medium [{}]", atta.getMedium().getName());
							machine.detachDevice(atta.getController(), atta.getPort(), atta.getDevice());
							machine.saveSettings();
							atta.getMedium().close();
						}
					}
					unlockSession(session);
					// unregister VM
					machine = getIMachineReference(vm.getId());
					if (machine == null) {
						LOG.error("Machine disapears... (?)");
					} else {
						machine.unregister(CleanupMode.Full);
						unlockSession(session);
					}
				}
			} catch (Exception e) {
				throw new VmException("", e);
			}
		}
	}

	public VmProductState getProductState(Vm vm) {
		if (vm == null) {
			return VmProductState.ERROR;
		} else {
			return getProductState(cache.getState(vm.getId()));
		}
	}

	public void save(Vm vm, VmModel model) throws VmException {
		synchronized (wsLock) {
			try {
				// get IMachine reference
				IMachine machine = getIMachineReference(vm.getId());
				ISession session = mgr.getSessionObject();
				// lock IMachine
				machine.lockMachine(session, LockType.Shared);
				machine = session.getMachine();
				try {
					// save IMachine
					IProgress progress = session.getConsole().saveState();
					// wait until completed
					while (!progress.getCompleted()) {
						// notify progress to model listeners
						LOG.debug("save progress [{}%]", progress.getOperationPercent());
						vm.setProgress(progress.getOperationPercent().intValue());
						model.fireVmStateUpdatedEvent(vm);
						Thread.sleep(500);
					}
					// rename saved state file
					LOG.debug("VM save completed. Rename snapshot");
					File mp = vm.getRuntimeContainer().getMountpoint();
					ShellUtil su = new ShellUtil();
					su.run(null, ShellUtil.NO_TIMEOUT, "mv", "-f", //
							new File(mp, "{" + vm.getId() + "}.sav").getAbsolutePath(), //
							new File(mp, "state.sav").getAbsolutePath());
					// discard state or we won't be able to unregister it
					machine = lockMachine(vm.getId(), session);
					session.getConsole().discardSavedState(true);
				} finally {
					// ensure that session is unlocked
					unlockSession(session);
				}
				//
				vm.setProgress(100);
				model.fireVmStateUpdatedEvent(vm);
			} catch (Exception e) {
				throw new VmException("", e);
			}
		}
	}

	/**
	 * Sometimes session locking could return 'The given session is busy'. In
	 * this case just try once again.
	 */
	private IMachine lockMachine(String id, ISession session) throws Exception {
		Exception last = null;
		for (int i = 0; i < API_RETRY; i++) {
			try {
				IMachine machine = getIMachineReference(id);
				machine.lockMachine(session, LockType.Shared);
				machine = session.getMachine();
				return machine;
			} catch (Exception e) {
				last = e;
				LOG.error("Failed to lock machine [" + id + "]. Retry " + API_RETRY + " times.", e);
				unlockSession(session);
				Thread.sleep(500);
			}
		}
		throw last;
	}

	public boolean isConnected() {
		return connected;
	}

	public void reconnect() {
		// restart virtualbox webservice in a new thread (sometimes it just stop
		// working)
		new Thread(new Runnable() {
			@Override
			public void run() {
				ShellUtil su = new ShellUtil();
				try {
					su.run(null, 0, "pkill", "-9", "vboxwebsrv");
				} catch (Exception e) {
					LOG.error("Failed to kill vboxwebsrv");
				}
				try {
					su.run(null, 0, "VBoxManage", "setproperty", "websrvauthlibrary", "null");
					su.run(null, 0, "vboxwebsrv");
				} catch (Exception e) {
					LOG.error("Failed to start vboxwebsrv");
				}
				LOG.error("Thread vboxsrv exit");
			}
		}, "vboxwebsrv").start();
		// start webclient service in a new thead too
		new Thread(new Runnable() {
			@Override
			public void run() {
				// Connect VirtualBox Web Service in a new thread to avoid
				// blocking login for 5 seconds.
				try {
					mgr = VirtualBoxManager.createInstance(null);
					mgr.connect("http://localhost:18083", "", "");
					vbox = mgr.getVBox();
					connected = true;
					LOG.info("VirtualBox Web Service connected [{}]", vbox.getVersion());
				} catch (Exception e) {
					LOG.error("VirtualBox Web Service not connected (http://localhost:18083). Is vboxwebsrv running? [" + e.getMessage() + "]", e);
				}
			}
		}, "WebService Connector").start();
	}

	public void listUsb(Vm vm, UsbDeviceEntryList list) throws VmException {
		try {
			// get IMachine reference
			IMachine machine = getIMachineReference(vm.getId());
			ISession session = mgr.getSessionObject();
			// lock IMachine
			machine.lockMachine(session, LockType.Shared);
			machine = session.getMachine();
			try {
				IConsole console = session.getConsole();
				// get list of usb from host
				List<IHostUSBDevice> hostDevices = vbox.getHost().getUSBDevices();
				// get list of attached usb devices
				HashSet<String> attachedIds = new HashSet<String>();
				for (IUSBDevice dev : console.getUSBDevices()) {
					attachedIds.add(dev.getAddress());
				}
				// populate result list
				for (IHostUSBDevice dev : hostDevices) {
					// exclude GemPlus product from the list in order to avoid
					// attaching the main smart-card reader to a guest
					if (dev.getVendorId() == 0x08e6) {
						continue;
					}
					// exclude product without description
					String p = dev.getProduct();
					String m = dev.getManufacturer();
					if (p == null || p.trim().length() == 0) {
						continue;
					}
					// format description : use product description and include
					// manufacturer if available
					String fmt = p;
					if (m != null && m.trim().length() > 0) {
						fmt += " (" + m + ")";
					}
					// add to list
					if (attachedIds.contains(dev.getAddress())) {
						list.add(new UsbDeviceEntry(vm.getId(),//
								new UsbDevice(dev.getId(), Integer.toHexString(dev.getVendorId()), Integer.toHexString(dev.getProductId()), fmt),//
								DeviceEntryState.ALREADY_ATTACHED));
					} else {
						list.add(new UsbDeviceEntry(vm.getId(),//
								new UsbDevice(dev.getId(), Integer.toHexString(dev.getVendorId()), Integer.toHexString(dev.getProductId()), fmt),//
								DeviceEntryState.AVAILABLE));
					}
				}
			} finally {
				// ensure that session is unlocked
				unlockSession(session);
			}
		} catch (Exception e) {
			throw new VmException("Failed to list USB devices", e);
		}
	}

	public void attachUsb(Vm vm, UsbDevice dev) throws VmException {
		try {
			// get IMachine reference
			IMachine machine = getIMachineReference(vm.getId());
			ISession session = mgr.getSessionObject();
			// lock IMachine
			machine.lockMachine(session, LockType.Shared);
			machine = session.getMachine();
			try {
				session.getConsole().attachUSBDevice(dev.getId());
			} finally {
				// ensure that session is unlocked
				unlockSession(session);
			}
		} catch (Exception e) {
			throw new VmException("Failed to attach USB device [" + dev.getId() + "]", e);
		}
	}

	public void detachUsb(Vm vm, UsbDevice dev) throws VmException {
		try {
			// get IMachine reference
			IMachine machine = getIMachineReference(vm.getId());
			ISession session = mgr.getSessionObject();
			// lock IMachine
			machine.lockMachine(session, LockType.Shared);
			machine = session.getMachine();
			try {
				session.getConsole().detachUSBDevice(dev.getId());
			} finally {
				// ensure that session is unlocked
				unlockSession(session);
			}
		} catch (Exception e) {
			throw new VmException("Failed to detach USB device [" + dev.getId() + "]", e);
		}
	}
}
