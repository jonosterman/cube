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

import java.util.ResourceBundle;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test fetching bundle i18n messages.
 */
public class BundleTest {
	@Test
	public void testBundle() {
		ResourceBundle bundle = CubeCommonResourceBundleProvider.getBundle();
		Assert.assertNotNull("localized message not found.", bundle.getString("test.message"));
	}
}
