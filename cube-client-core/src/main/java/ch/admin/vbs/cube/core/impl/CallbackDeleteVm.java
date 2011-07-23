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
