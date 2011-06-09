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

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * RessourceBundle (i18n strings) loader.
 */
public final class CubeCommonResourceBundleProvider {
	private static final String BUNDLE_NAME = "CubeCommon_bundle";

	/**
	 * The ResourceBundleProvider is an utility class with no constructor!
	 */
	private CubeCommonResourceBundleProvider() {
	}

	/**
	 * Returns the default resource bundle.
	 * 
	 * @return the default resource bundle
	 */
	public static ResourceBundle getBundle() {
		return ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());
	}
}
