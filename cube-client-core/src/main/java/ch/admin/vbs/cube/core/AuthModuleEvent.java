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

import java.security.KeyStore;
import java.security.KeyStore.Builder;

/**
 * @see IAuthModule
 */
public class AuthModuleEvent {
	private final AuthEventType type;
	private final KeyStore keystore;
	private final char[] pwd;
	private final Builder builder;

	public enum AuthEventType {
		SUCCEED, FAILED, FAILED_CARDTIMEOUT, FAILED_WRONGPIN, FAILED_CANCELED
	}

	public AuthModuleEvent(AuthEventType type, KeyStore keystore, Builder builder, char[] pwd) {
		this.type = type;
		this.keystore = keystore;
		this.builder = builder;
		this.pwd = pwd;
	}

	public AuthEventType getType() {
		return type;
	}

	public KeyStore getKeystore() {
		return keystore;
	}

	public char[] getPassword() {
		return pwd;
	}

	public Builder getBuilder() {
		return builder;
	}
}
