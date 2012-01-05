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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;
import ch.admin.vbs.cube.core.impl.scauthmodule.AbstractState.ScAuthStateTransition;

class StateWatchdog implements Runnable {
	/**
	 * 
	 */
	private final ScAuthModule scAuthModule;
	private static final Logger LOG = LoggerFactory.getLogger(StateWatchdog.class);
	
	/**
	 * @param scAuthModule
	 */
	public StateWatchdog(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	@Override
	public void run() {
		while (this.scAuthModule.running) {
			// get a reference on active state
			AbstractState tstate = this.scAuthModule.activeState;
			// test if deadline is defined and expired
			if (tstate != null && tstate.deadline != 0 && tstate.deadline < System.currentTimeMillis()) {
				LOG.debug("Abort state [{}] due to timeout", tstate);
				tstate.deadline = 0; // reset deadline to
				if (tstate == this.scAuthModule.states.waitPasswordState) {
					this.scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED_USERTIMEOUT, null, null, null));
				} else {
					this.scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED_CARDTIMEOUT, null, null, null));
				}
				this.scAuthModule.enqueue(ScAuthStateTransition.ABORT_AUTH);
			}
			// log..
			if (tstate != null && tstate.deadline > 0) {
				LOG.debug("Monitor state [{}] : remaining {} ms", tstate, tstate.deadline - System.currentTimeMillis());
			}
			// sleep
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOG.error("", e);
			}
		}
	}
}