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

package ch.admin.vbs.cube.common;

/**
 * Allows to access cube-common project's configuration file easily.
 * 
 * @see Singleton Pattern
 */
public class CubeCommonProperties extends AbstractProperties {
	private static final CubeCommonProperties instance = new CubeCommonProperties();

	/**
	 * Constructor.
	 */
	private CubeCommonProperties() {
		super("cube-common.config.file", "cube-common.properties.xml", "cube-common.properties-test.xml");
	}

	/**
	 * @param key
	 *            the property key.
	 * @return value associated to this key in configuration file
	 */
	public static String getProperty(String key) {
		return instance.getInternProperty(key);
	}
}
