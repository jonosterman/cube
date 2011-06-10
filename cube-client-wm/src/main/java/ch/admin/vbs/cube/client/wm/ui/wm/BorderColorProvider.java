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

package ch.admin.vbs.cube.client.wm.ui.wm;

import java.awt.Color;
import java.util.HashMap;

import ch.admin.vbs.cube.common.CubeClassification;

/**
 */
public class BorderColorProvider {
	// UNCLASSIFIED, RESTRICTED, CONFIDENTIAL, SECRET
	private static final HashMap<CubeClassification, Color> COLOR_SELECTED = new HashMap<CubeClassification, Color>();
	static {
		COLOR_SELECTED.put(CubeClassification.SECRET, colorToWhite(Color.RED, 1));
		COLOR_SELECTED.put(CubeClassification.UNCLASSIFIED, colorToWhite(Color.GREEN.darker().darker(), 1));
		COLOR_SELECTED.put(CubeClassification.CONFIDENTIAL, colorToWhite(Color.YELLOW.darker().darker(), 1));
		COLOR_SELECTED.put(CubeClassification.RESTRICTED, colorToWhite(Color.BLUE, 1));
	}
	private static final Color DEFAULT_BACKGROUND_COLOR = Color.RED;

	public static Color getBackgroundColor(final CubeClassification classification) {
		Color color = DEFAULT_BACKGROUND_COLOR;
		if (classification != null) {
			color = COLOR_SELECTED.get(classification);
		}
		return color;
	}

	private static Color colorToWhite(Color c, int iter) {
		float[] cp = c.getColorComponents(null);
		for (int n = 0; n < iter; n++) {
			for (int i = 0; i < cp.length; i++) {
				cp[i] = (1.0f + cp[i]) / 2;
			}
		}
		return new Color(cp[0], cp[1], cp[2]);
	}
}
