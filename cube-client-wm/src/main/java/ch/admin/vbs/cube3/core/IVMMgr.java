package ch.admin.vbs.cube3.core;

/** handle VM management. Including mounting, vpn, etc.. */
public interface IVMMgr {
	static enum Command { START, STOP, PAUSE, STAGE, CUSTOMIZE  }
	
	void command(VirtualMachine vm, Command cmd);
	
}
