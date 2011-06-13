package ch.admin.vbs.cube.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.core.AbstractUICallback;
import ch.admin.vbs.cube.core.I18nBundleProvider;
import ch.admin.vbs.cube.core.IClientFacade;
import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISessionManager;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmStatus;

public class CallbackShutdown extends AbstractUICallback implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(CallbackShutdown.class);
	private final ISessionManager smanager;
	private IClientFacade clientFacade;
	private Executor exec = Executors.newCachedThreadPool();

	public CallbackShutdown(ISessionManager smanager,IClientFacade clientFacade) {
		this.smanager = smanager;
		this.clientFacade = clientFacade;
	}

	@Override
	public void process() {
		new Thread(this).start();
	}
	
	@Override
	public void run() {
		// display shutdown message
		clientFacade.showMessage("cube.shutdown", IClientFacade.OPTION_NONE);
		// try to save all running VMs of all users
		List<ISession> sessions = smanager.getSessions();
		List<Vm> vmToWait = new ArrayList<Vm>();
		for (final ISession s : sessions) {
			for (final Vm vm : s.getModel().getVmList()) {
				if (vm.getVmStatus() == VmStatus.RUNNING) {
					vmToWait.add(vm);
				}
			}
			exec.execute(new Runnable() {
				@Override
				public void run() {
					smanager.closeSession(s);
				}
			});
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
					clientFacade.showMessage(I18nBundleProvider.getBundle().getString("login.waitstopped") + " (" + remind + ")", IClientFacade.OPTION_NONE);
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
	public void aborted() {
	}
}
