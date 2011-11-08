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

/**
 * Mother class of all states.
 */
abstract class AbstractState {
	protected long deadline = 0;

	/**
	 * State transitions
	 */
	public static enum ScAuthStateTransition {
		TIMEOUT, // State timeout expiration
		START_AUTH, // Start authentication
		ABORT_AUTH, // Abort authentication
		PASSWORD_SUBMIT, // User submitted his password
		PASSWORD_REQUEST, // KeyStore request user's password
		KEYSTORE_READY;// KeyStore successfully opened
	}

	/**
	 * Apply the given transition to the current state.
	 * 
	 * @return the next state
	 */
	public AbstractState transition(ScAuthStateTransition trs) {
		// default implementation
		ScAuthModule.LOG.error("Invalid transition [{}] [{}]. Ignore.", trs, this);
		return this;
	}

	/**
	 * Proceed state logic here.
	 */
	public void proceed() {
		// default implementation
	}

	/**
	 * Reset state timeout. This timeout will be monitored and enforced by the
	 * StateWatchdog class. Set '0' if state may stay active without time limit.
	 */
	protected void resetTimeout(long timeout) {
		// update deadline with the new timeout
		deadline = timeout == 0 ? 0 : System.currentTimeMillis() + timeout;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}