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


class WaitKeystoreAndPasswordState extends AbstractState {
	/**
	 * 
	 */
	private final ScAuthModule scAuthModule;

	/**
	 * @param scAuthModule
	 */
	public WaitKeystoreAndPasswordState(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	@Override
	public void proceed() {
		resetTimeout(ScAuthModule.TIMEOUT_KEYSTOREINIT);
		scAuthModule.exec.execute(scAuthModule.getOpenKeyStoreTask());
	}

	@Override
	public AbstractState transition(ScAuthStateTransition trs) {
		switch (trs) {
		case PASSWORD_SUBMIT:
			return scAuthModule.states.waitKeyStore;
		case PASSWORD_REQUEST:
			return scAuthModule.states.waitPasswordState;
		case ABORT_AUTH:
			return scAuthModule.states.idle;
		default:
			return super.transition(trs);
		}
	}
}