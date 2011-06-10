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

import org.junit.Test;

import ch.admin.vbs.cube.common.container.SizeFormatUtil;

/**
 * Test keyring creation and usage. Use CubeKeyring with DmCrypt encryption and
 * a test P12 file.
 * 
 * 
 * 
 */
public class SizeFormatterTest {
	private static final long KILO = 1024;
	private static final long MEGA = KILO * 1024;
	private static final long GIGA = MEGA * 1000;
	private static final long TERA = GIGA * 1000;

	@Test
	public void testFormatting() throws Exception {
		fmt(123);
		fmt(KILO);
		fmt(KILO + 123);
		fmt(MEGA);
		fmt(MEGA + KILO * 330);
		fmt(GIGA);
		fmt(GIGA + MEGA * 3300);
		fmt(TERA);
		fmt(TERA + GIGA * 33000);
		fmt(7594676019l);
	}

	public void fmt(long l) {
		System.out.printf("Format [%d] as '%s'\n", l, SizeFormatUtil.format(l));
	}

	public static void main(String[] args) throws Exception {
		SizeFormatterTest t = new SizeFormatterTest();
		t.testFormatting();
	}
}
