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

package ch.admin.vbs.cube.common.keyring.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeCommonProperties;
import ch.admin.vbs.cube.common.keyring.IKeyring;

/**
 * Create a keyring based on configuration file parameter 'keyring.keyringImpl'.
 */
public class KeyringProvider {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(KeyringProvider.class);
	private static final String KEYRING_PROVIDER_KEYRING_IMPL_PROP = "keyring.keyringImpl";
	private static KeyringProvider instance = new KeyringProvider();
	private IKeyring keyring;

	public static KeyringProvider getInstance() {
		return instance;
	}

	private KeyringProvider() {
		String classname = CubeCommonProperties.getProperty(KEYRING_PROVIDER_KEYRING_IMPL_PROP);
		if (classname == null) {
			LOG.info("no keyring implementation specified in config file [{}=...]", KEYRING_PROVIDER_KEYRING_IMPL_PROP);
			return;
		}
		try {
			keyring = (IKeyring) Class.forName(classname).newInstance();
			LOG.debug("Keyring initialized [{}]", classname);
		} catch (Exception e) {
			LOG.error("Could not initialize keyring [" + classname + "]", e);
		}
	}

	public IKeyring getKeyring() {
		return keyring;
	}
}
