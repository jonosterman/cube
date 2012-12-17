package ch.admin.vbs.cube3.core;

import ch.admin.vbs.cube.common.keyring.IIdentityToken;

public interface IWSConnection {
	public void stop();

	public void restart(IIdentityToken id);
}
