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

package ch.admin.vbs.cube.client.wm.xrandx;

import java.util.List;

public class XRScreen {
	public enum State { CONNECTED, CONNECTED_AND_ACTIVE, DISCONNECTED }
	private final String id;
	private final State state;
	private final int posy;
	private final int posx;
	private final XRResolution selectedResolution;
	private final String selectedFrequency;
	private final List<XRResolution> resolutions;

	@Override
	public String toString() {
		return String.format("[%s] state:%s , %dx%d", id, state, selectedResolution == null ? -1 : selectedResolution.width, selectedResolution == null ? -1
				: selectedResolution.height);
	}

	public XRScreen(String id, State state, int posx, int posy, List<XRResolution> resolutions, XRResolution selectedResolution, String selectedFrequency) {
		this.id = id;
		this.state = state;
		this.posx = posx;
		this.posy = posy;
		this.resolutions = resolutions;
		this.selectedResolution = selectedResolution;
		this.selectedFrequency = selectedFrequency;
	}

	public static class XRResolution {
		private final int width;
		private final List<String> freqs;
		private final int height;

		public XRResolution(int width, int height, List<String> freqs) {
			this.width = width;
			this.freqs = freqs;
			this.height = height;
		}
	}

	public String getId() {
		return id;
	}

	public int getPosY() {
		return posy;
	}

	public int getPosX() {
		return posx;
	}

	public int getCurrentWidth() {
		if (selectedResolution == null) {
			return 0;
		} else {
			return selectedResolution.width;
		}
	}

	public int getCurrentHeight() {
		if (selectedResolution == null) {
			return 0;
		} else {
			return selectedResolution.height;
		}
	}

	public XRResolution getSelectedResolution() {
		return selectedResolution;
	}

	public String getSelectedFrequency() {
		return selectedFrequency;
	}

	public State getState() {
		return state;
	}

	public List<XRResolution> getResolutions() {
		return resolutions;
	}
}
