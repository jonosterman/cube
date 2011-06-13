package ch.admin.vbs.cube.core.impl;

import ch.admin.vbs.cube.core.AbstractUICallback;
import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISession.VmCommand;

public class CallbackDeleteVm extends AbstractUICallback {
	private final ISession session;
	private final String vmId;

	public CallbackDeleteVm(String vmId, ISession session) {
		this.vmId = vmId;
		this.session = session;
	}

	@Override
	public void process() {
		session.controlVm(vmId, VmCommand.DELETE, null);
	}

	@Override
	public void aborted() {
	}
}
