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

package ch.admin.vbs.cube.common.container;

/**
 * Helper class used to format human readable file size.
 */
public class SizeFormatUtil {
	private static final double KILO = 1024;
	private static final double MEGA = KILO * 1024;
	private static final double GIGA = MEGA * 1000;
	private static final double TERA = GIGA * 1000;

	/**
	 * Depending on the size, return a size formatted in bytes, K, M, G or T
	 * bytes.
	 * 
	 * @param bytes
	 *            size in bytes
	 * @return human readable value
	 */
	public static String format(long bytes) {
		if (bytes < 0) {
			return "invalid input(" + bytes + ")";
		}
		if (bytes < 1.0) {
			return String.format("%d byte", bytes);
		} else if (bytes < KILO) {
			return String.format("%d bytes", bytes);
		} else if (bytes < MEGA) {
			return String.format("%.1fK", bytes / KILO);
		} else if (bytes < GIGA) {
			return String.format("%.1fM", bytes / MEGA);
		} else if (bytes < TERA) {
			return String.format("%.1fG", bytes / GIGA);
		} else {
			return String.format("%.1fT", bytes / TERA);
		}
	}
}
