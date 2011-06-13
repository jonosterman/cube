package ch.admin.vbs.cube.core.impl;

import ch.admin.vbs.cube.core.AbstractUICallback;
import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISession.VmCommand;
import ch.admin.vbs.cube.core.usb.UsbDevice;

public class CallbackUsb extends AbstractUICallback {

	protected UsbDevice device;
	private final ISession session;
	private final String vmId;

	public CallbackUsb(ISession session, String vmId) {
		this.session = session;
		this.vmId = vmId;
	}

	public void setDevice(UsbDevice device ) {
		this.device = device;
	}
	
	@Override
	public void process() {
		session.controlVm(vmId, VmCommand.ATTACH_USB, device);
	}

	@Override
	public void aborted() {
	}
}
