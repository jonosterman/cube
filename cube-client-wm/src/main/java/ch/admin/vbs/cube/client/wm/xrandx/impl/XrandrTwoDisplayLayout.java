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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;

public class XrandrTwoDisplayLayout {
	private static final Logger LOG = LoggerFactory.getLogger(XrandrTwoDisplayLayout.class);

	public enum Layout {
		AB, BA, A, B
	}

	/** update posx & posy in order to match choosen layout. */
	public void layout(Layout layout, IXrandr xrandr) {
		LOG.debug("Apply layout [{}] (2 pass, reload xrandx in between)", layout);
		// 2 pass layout (since we first have to activate before getting the
		// screen resolution)
		// - 1st : Activate/deactivate screen
		// - reload config
		// - 2st : set x and y offset
		// - reload config so xrandx cache is up-to-date
		for (int pass = 0; pass < 2; pass++) {
			// filter disconnected screen.
			xrandr.reloadConfiguration();
			List<XRScreen> screens = xrandr.getScreens();
			ArrayList<XRScreen> conn = new ArrayList<XRScreen>();
			for (XRScreen x : screens) {
				if (x.getState() != State.DISCONNECTED) {
					conn.add(x);
				}
			}
			// compute 'x' and 'active' for each screen.
			int x = 0;
			switch (layout) {
			case A:
				for (int i = 0; i < conn.size(); i++) {
					xrandr.setScreen(conn.get(i), i == 0, x, 0);
					x += conn.get(i).getCurrentWidth();
				}
				break;
			case B:
				for (int i = 0; i < conn.size(); i++) {
					xrandr.setScreen(conn.get(i), i == conn.size() - 1, x, 0);
					x += conn.get(i).getCurrentWidth();
				}
				break;
			case AB:
				for (int i = 0; i < conn.size(); i++) {
					xrandr.setScreen(conn.get(i), true, x, 0);
					x += conn.get(i).getCurrentWidth();
				}
				break;
			case BA:
				for (int i = conn.size()-1; i >= 0; i--) {
					xrandr.setScreen(conn.get(i), true, x, 0);
					x += conn.get(i).getCurrentWidth();
				}
				break;
			default:
				LOG.error("layout [{}] not supported", layout);
				break;
			}
		}
		xrandr.reloadConfiguration();
	}
}
