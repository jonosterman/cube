package ch.admin.vbs.cube.core.vm;

import ch.admin.vbs.cube.core.ISession.IOption;

public class NicOption implements IOption {
	private final String nic;

	public NicOption(String nic) {
		this.nic = nic;
	}
	
	public String getNic() {
		return nic;
	}
}
