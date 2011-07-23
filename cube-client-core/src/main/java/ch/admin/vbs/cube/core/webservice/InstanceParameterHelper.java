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

package ch.admin.vbs.cube.core.webservice;

import cube.cubemanager.services.InstanceConfigurationDTO;
import cube.cubemanager.services.InstanceConfigurationDTO.Parameters.Entry;

/**
 * InstanceDescriptorDTO are stored in a list instead a map. This class help to
 * retrieve key/value pair in a convenient way.
 * 
 * 
 * 
 */
public class InstanceParameterHelper {
	public static String getInstanceParameter(String key, InstanceConfigurationDTO instance) {
		for (Entry e : instance.getParameters().getEntry()) {
			if (key.equals(e.getKey())) {
				return e.getValue();
			}
		}
		return null;
	}

	public static long getInstanceParameterAsLong(String key, InstanceConfigurationDTO instance) {
		for (Entry e : instance.getParameters().getEntry()) {
			if (key.equals(e.getKey())) {
				return Long.parseLong(e.getValue());
			}
		}
		return 0l;
	}

	public static boolean getInstanceParameterAsBoolean(String key, InstanceConfigurationDTO instance) {
		for (Entry e : instance.getParameters().getEntry()) {
			if (key.equals(e.getKey())) {
				return "true".equalsIgnoreCase(e.getValue());
			}
		}
		return false;
	}

	public static int getInstanceParameterAsInteger(String key, InstanceConfigurationDTO instance) {
		for (Entry e : instance.getParameters().getEntry()) {
			if (key.equals(e.getKey())) {
				return Integer.parseInt(e.getValue());
			}
		}
		return 0;
	}
}
