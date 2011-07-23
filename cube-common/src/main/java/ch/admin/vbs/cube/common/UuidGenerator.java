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

package ch.admin.vbs.cube.common;

import java.util.Random;
import java.util.regex.Pattern;

/**
 * Generate random UUID in form of XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX . Used
 * to identify VDI, VMs, Containers, etc.
 */
public class UuidGenerator {
	private static char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static Random rnd = new Random();
	private static Pattern regex = Pattern.compile("^[0-9a-f-A-F]{8}-[0-9a-f-A-F]{4}-[0-9a-f-A-F]{4}-[0-9a-f-A-F]{4}-[0-9a-f-A-F]{12}$");

	public static String generate() {
		// 88888888-4444-4444-4444-11111111111
		char[] buffer = new char[36];
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = hexChars[rnd.nextInt(hexChars.length)];
		}
		buffer[8] = '-';
		buffer[13] = '-';
		buffer[18] = '-';
		buffer[23] = '-';
		return new String(buffer);
	}

	/**
	 * @param uuid
	 * @return true if uuid syntaxe matche template
	 */
	public static boolean validate(String uuid) {
		return regex.matcher(uuid).find();
	}
}
