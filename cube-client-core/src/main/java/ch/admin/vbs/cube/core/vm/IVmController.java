/**
 * Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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