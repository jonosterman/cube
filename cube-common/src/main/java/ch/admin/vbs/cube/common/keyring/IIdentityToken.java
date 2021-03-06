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

package ch.admin.vbs.cube.common.keyring;

import java.security.KeyStore.Builder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import ch.admin.vbs.cube.common.keyring.IIdentityToken.KeyType;

public interface IIdentityToken {
	enum KeyType {
		ENCIPHERMENT, SIGNATURE, AUTHENTIFICATION
	}

	/** @return last valid private key */
	PrivateKey getPrivatekey(KeyType type);

	/** @return last valid public key */
	PublicKey getPublickey(KeyType type);

	/** @return last all private key (some may be expired) */
	PrivateKey[] getAllPrivatekey(KeyType type);

	/** @return last all public key (some may be expired) */
	PublicKey[] getAllPublickey(KeyType type);

	/** @return a human readable subject's name */
	String getSubjectName();

	/**
	 * @return a unique string identifier for this USER (even if it replace or
	 *         renew its token)
	 */
	String getUuid();

	/** @return a one-way hash of the user's uuid. */
	String getUuidHash();

	/** needed to build SSL connections. */
	Builder getBuilder();

	X509Certificate getCertificate(KeyType type);
}