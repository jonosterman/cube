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

package test.old;

import net.cube.common.SizeFormatUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test keyring creation and usage. Use CubeKeyring with DmCrypt encryption and
 * a test P12 file.
 */
public class SizeFormatterTest {
	private static final long KILO = 1024;
	private static final long MEGA = KILO * 1024;
	private static final long GIGA = MEGA * 1000;
	private static final long TERA = GIGA * 1000;

	@Test
	public void testFormatting() throws Exception {
		fmt(123, "123 bytes");
		fmt(KILO, "1.0K");
		fmt(KILO + 123, "1.1K");
		fmt(MEGA, "1.0M");
		fmt(MEGA + KILO * 330, "1.3M");
		fmt(GIGA, "1.0G");
		fmt(GIGA + MEGA * 330, "1.3G");
		fmt(TERA, "1.0T");
		fmt(TERA + GIGA * 330, "1.3T");
		fmt(7594676019l, "7.2G");
		System.out.println("Test user friendly formating ..... OK");
	}

	public void fmt(long l, String control) {
		//System.out.printf("SizeFormatterTest: Format [%d] as '%s'\n", l, SizeFormatUtil.format(l));
		Assert.assertEquals(control, SizeFormatUtil.format(l));
	}
}
