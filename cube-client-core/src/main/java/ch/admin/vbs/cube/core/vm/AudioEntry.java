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
package ch.admin.vbs.cube.core.vm;

/**
 * Holds information about a channel volume configuration. It is not
 * synchronized with the effective configuration in pulseaudio and should
 * therefore be re-fetched each time.
 */
public class AudioEntry {
	private final int index;
	private final int volume;
	private final boolean muted;

	public AudioEntry(int index, int volume, boolean muted) {
		this.index = index;
		this.volume = volume;
		this.muted = muted;
	}

	public int getIndex() {
		return index;
	}

	public int getVolume() {
		return volume;
	}

	public boolean isMuted() {
		return muted;
	}
}