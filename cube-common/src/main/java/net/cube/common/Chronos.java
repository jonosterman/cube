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

package net.cube.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper used to track time along code execution (for debug purpose).
 */
public class Chronos {
	private long ts;
	private static final Logger LOG = LoggerFactory.getLogger(Chronos.class);

	public Chronos() {
		ts = System.currentTimeMillis();
	}

	public void zap(String message) {
		// compute time since last zap
		long now = System.currentTimeMillis();
		long delta = now - ts;
		ts = now;
		// print message
		if (delta > 2000) {
			LOG.debug(String.format("[duration: %.1f secs] %s", delta / 1000.0, message));
		} else if (delta > 1000) {
			LOG.debug(String.format("[duration: %.1f sec] %s", delta / 1000.0, message));
		} else {
			LOG.debug("[duration: {} ms] {}", delta, message);
		}
	}
}
