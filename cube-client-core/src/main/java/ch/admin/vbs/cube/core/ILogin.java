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

package ch.admin.vbs.cube.core;

import ch.admin.vbs.cube.common.keyring.IIdentityToken;

/**
 * ILogin manage the authentication of the user. It notify core when a user has
 * been authenticated (using a token/password) and when a user removed is token
 * (invalidate its credential).
 * 
 * It uses ILoginListener to notify it.
 * 
 * It uses ILoginUI to display UI elements to user.
 */
public interface ILogin {
	/** Activate ILogin process */
	void start();

	/** Lock user */
	void discardAuthentication(IIdentityToken id);

	/** Listeners */
	void addListener(ILoginListener l);

	/** Listeners */
	void removeListener(ILoginListener l);
}
