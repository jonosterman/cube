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

/**
 * Provide the interface of a keystore provider.
 */
public interface IAuthModule {
	/**
	 * Always invoke 'start()' once at the begining in order to activate the
	 * module.
	 */
	void start();

	/**
	 * Open the token. If the token was already opened completly or partially,
	 * It will re-initilize it.
	 */
	void openToken();

	/** Set password. */
	void setPassword(char[] password);

	/** close or abort token opening */
	void abort();

	/** subscribe for auth event like 'KeyStore opened'. */
	void addListener(IAuthModuleListener l);
}
