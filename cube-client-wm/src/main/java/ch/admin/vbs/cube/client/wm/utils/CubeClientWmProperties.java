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

package ch.admin.vbs.cube.client.wm.utils;

import ch.admin.vbs.cube.common.AbstractProperties;

/**
 * This abstract class represents the central configuration for Cube Common.
 * 
 * 
 */
public class CubeClientWmProperties extends AbstractProperties {
	private static final CubeClientWmProperties instance = new CubeClientWmProperties();

	/**
	 * Private constructor (singleton pattern)
	 */
	private CubeClientWmProperties() {
		super("cube-client-wm.config.file", "cube-client-wm.properties.xml", "cube-client-wm.properties-test.xml");
	}

	/**
	 * Searches for the property with the specified key in this property list.
	 * 
	 * @param key
	 *            - the property key.
	 * @return the value in this property list with the specified key value.
	 */
	public static String getProperty(String key) {
		return instance.getInternProperty(key);
	}
}
