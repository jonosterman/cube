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

package ch.admin.vbs.cube.client.wm.xrandx.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;

public class XrandrTwoDisplayLayout {
	private static final Logger LOG = LoggerFactory.getLogger(XrandrTwoDisplayLayout.class);

	public enum Layout {
		AB, BA, A, B
	}

	/** update posx & posy in order to match choosen layout. */
	public void layout(Layout layout, IXrandr xrandr) {
		List<XRScreen> screens = xrandr.getScreens();
		// actually layout display a way it support also more or less than 2
		// screens.
		int x = 0;
		switch (layout) {
		case A:
			// activate only first screen an move it at position 0x0
			for (int i = 0; i < screens.size(); i++) {
				if (!"connected".equals(screens.get(i).getState())) {
					continue;
				}
				if (i == 0) {
					xrandr.setScreen(screens.get(i), true, x, 0);
				} else {
					xrandr.setScreen(screens.get(i), false, x, 0);
				}
				x += screens.get(i).getCurrentWidth();
			}
			break;
		case B:
			// activate only last screen an move it at position 0x0
			for (int i = screens.size() - 1; i >= 0; i--) {
				if (!"connected".equals(screens.get(i).getState())) {
					continue;
				}
				if (i == screens.size() - 1) {
					xrandr.setScreen(screens.get(i), true, x, 0);
				} else {
					xrandr.setScreen(screens.get(i), false, x, 0);
				}
				x += screens.get(i).getCurrentWidth();
			}
			break;
		case AB:
			// activate all screens an move layout them side by side
			for (int i = 0; i < screens.size(); i++) {
				if (!"connected".equals(screens.get(i).getState())) {
					continue;
				} else {
					xrandr.setScreen(screens.get(i), true, x, 0);
					x += screens.get(i).getCurrentWidth();
				}
			}
			break;
		case BA:
			// activate all screens an move layout them side by side in reverse
			// order
			for (int i = screens.size() - 1; i >= 0; i--) {
				if (!"connected".equals(screens.get(i).getState())) {
					continue;
				} else {
					xrandr.setScreen(screens.get(i), true, x, 0);
					x += screens.get(i).getCurrentWidth();
				}
			}
			break;
		default:
			LOG.error("layout [{}] not supported", layout);
			break;
		}
	}
}
