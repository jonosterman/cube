package ch.admin.vbs.cube3.core;

public interface IVBoxMgr {
	enum VBoxCommand { REGISTER_AND_POWER_ON, POWER_OFF_AND_CLEANUP, STANDBY_AND_CLEANUP, CONFIGURE }
	
	void command(VirtualMachine vm, VBoxCommand cmd);
}
