/**
 * Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package ch.admin.vbs.cube.core;

import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.core.vm.VmModel;

/**
 * Cube is multi-session oriented: several user may work on the system at the
 * same time. The session holds user-specific resources.
 * 
 * The session user is linked via the IIdentityToken. It may be partialy
 * invalidate if the user removed its token and reseted when it insert its token
 * again.
 */
public interface ISession {
	/**
	 * VM command is used between CubeCore and ISession to avoid implementing
	 * each time many methods like startVm/stopVm/... Instead a single
	 * controlVm(VmCommand) method. This way to do may also be used between
	 * ICoreFacad and IClientFacad (if we have time to do and test it).
	 */
	public enum VmCommand {
		START, POWER_OFF, SAVE, STAGE, DELETE, TRANSFER_FILE, INSTALL_GUESTADDITIONS, ATTACH_USB, DETACH_USB, LIST_USB
	}

	IIdentityToken getId();

	void setId(IIdentityToken id);

	void close();

	void setContainerFactory(IContainerFactory containerFactory);

	void lock();

	void open();

	VmModel getModel();

	void controlVm(String vmId, VmCommand cmd, IOption option);
	
	public static interface IOption {
		
	}
}
