package ch.admin.vbs.cube.core.usb;

public class UsbDeviceEntry {
	
	private final DeviceEntryState state;
	private final String vmId;
	private final UsbDevice device;

	public enum DeviceEntryState { AVAILABLE, ALREADY_ATTACHED, ATTACHED_TO_ANOTHER_VM }

	public UsbDeviceEntry(String vmId, UsbDevice device, DeviceEntryState state) {
		this.vmId = vmId;
		this.device = device;
		this.state = state;
	}

	public DeviceEntryState getState() {
		return state;
	}

	public String getVmId() {
		return vmId;
	}

	public UsbDevice getDevice() {
		return device;
	}
	
	@Override
	public String toString() {
		return device.toString();
	}
	
}
