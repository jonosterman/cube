package ch.admin.vbs.cube.core.usb;

import java.util.ArrayList;

import ch.admin.vbs.cube.core.ISession.IOption;

public class UsbDeviceEntryList extends ArrayList<UsbDeviceEntry> implements IOption {

	private boolean updated;
	
	private static final long serialVersionUID = 1L;

	public void setUpdated(boolean updated) {
		this.updated = updated;
	}

	public boolean isUpdated() {
		return updated;
	}
}
