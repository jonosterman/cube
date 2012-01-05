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

package ch.admin.vbs.cube.core.impl.scauthmodule;

import ch.admin.vbs.cube.core.AuthModuleEvent;

/**
 * Initial state. It will transit to "wait password and keystore" state as soon
 * as it receive a START_AUTH event (via IAuthModule.openToken()).
 */
class IdleState extends AbstractState {
	private final ScAuthModule scAuthModule;

	/**
	 * @param scAuthModule
	 */
	public IdleState(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	@Override
	public void proceed() {
		// We may rest in this state indefinitely
		resetTimeout(ScAuthModule.TIMEOUT_NO);
		// check if 'abortReason' is set and display a dialog accordingly.
		AuthModuleEvent event = scAuthModule.getAbortReason();
		if (event != null) {
			this.scAuthModule.fireStateChanged(event);
		}
	}

	@Override
	public AbstractState transition(ScAuthStateTransition trs) {
		switch (trs) {
		case START_AUTH:
			return scAuthModule.states.waitKeyStoreAndPassword;
		case ABORT_AUTH:
			return scAuthModule.states.idle;
		default:
			return super.transition(trs);
		}
	}
}