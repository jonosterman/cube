package ch.admin.vbs.cube3.core.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;
import ch.admin.vbs.cube3.core.IVBoxMgr;
import ch.admin.vbs.cube3.core.VirtualMachine;

public class VBoxMgr implements IVBoxMgr {
	private static final Logger LOG = LoggerFactory.getLogger(VBoxMgr.class);

	@Override
	public void command(VirtualMachine vm, VBoxCommand cmd) {
		ShellUtil su = new ShellUtil();
		switch (cmd) {
		case REGISTER_AND_POWER_ON:
			try {
				su.run("VBoxManage", "startvm", vm.getId());
			} catch (ShellUtilException e) {
				LOG.error("Failed to start VM");
			}
			break;
		case STANDBY_AND_CLEANUP:
			break;
		case POWER_OFF_AND_CLEANUP:
			try {
				su.run("VBoxManage", "poweroff", vm.getId());
			} catch (ShellUtilException e) {
				LOG.error("Failed to start VM");
			}
			break;
		case CONFIGURE:
			break;
		default:
			LOG.error("Unrecognized command [{}]", cmd);
			break;
		}
	}
}
