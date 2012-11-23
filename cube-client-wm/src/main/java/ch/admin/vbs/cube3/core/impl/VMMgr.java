package ch.admin.vbs.cube3.core.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube3.core.IVBoxMgr;
import ch.admin.vbs.cube3.core.IVBoxMgr.VBoxCommand;
import ch.admin.vbs.cube3.core.IVMMgr;
import ch.admin.vbs.cube3.core.VirtualMachine;

public class VMMgr implements IVMMgr{
	private static final Logger LOG = LoggerFactory.getLogger(VMMgr.class);
	private IVBoxMgr vboxMgr;
	@Override
	public void command(VirtualMachine vm, Command cmd) {
		switch (cmd) {
		case START:
			vboxMgr.command(vm, VBoxCommand.REGISTER_AND_POWER_ON);
			break;
		case STOP:
			vboxMgr.command(vm, VBoxCommand.POWER_OFF_AND_CLEANUP);
			break;
		case PAUSE:
			break;
		case STAGE:
			break;
		case CUSTOMIZE:
			break;
		default:
			LOG.error("Unrecognized command [{}]",cmd);
			break;
		}
	}
	
	public void setup(IVBoxMgr vboxMgr) {
		this.vboxMgr = vboxMgr;
	}
}
