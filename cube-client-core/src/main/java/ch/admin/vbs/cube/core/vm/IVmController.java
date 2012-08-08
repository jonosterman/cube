
package ch.admin.vbs.cube.core.vm;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.core.ISession.IOption;
import ch.admin.vbs.cube.core.ISession.VmCommand;

public interface IVmController {

	public abstract void controlVm(final Vm vm, final VmModel model, VmCommand cmd, final IIdentityToken id, final IKeyring keyring, final Container transfer,
			final IOption option);

	public abstract void start();

	public abstract void registerVmModel(VmModel vmModel);

	public abstract void unregisterVmModel(VmModel vmModel);

	public abstract Vm findVmById(String id);

	public abstract void refreshVmState(Vm vm);

	public abstract void setTempStatus(Vm vm, VmState stopping);

	public abstract VmState clearTempStatus(Vm vm);
}