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

package ch.admin.vbs.cube.core.usb;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.common.shell.ShellUtil;

public class UsbManager {
	private static final Logger LOG = LoggerFactory.getLogger(UsbManager.class);

	public List<UsbDevice> listDevices() throws CubeException {
		try {
			ScriptUtil script = new ScriptUtil();
			ShellUtil su = script.execute("./usb-list.pl");
			String[] lines = su.getStandardOutput().toString().split("\\n");
			ArrayList<UsbDevice> devices = new ArrayList<UsbDevice>();
			for (String line : lines) {
				String[] tokens = line.split(":", 3);
				if (tokens.length == 3) {
					devices.add(new UsbDevice(tokens[0].toUpperCase(), tokens[1].toUpperCase(), tokens[2]));
				} else {
					LOG.warn("Ignore usb device [{}][{}]", line, tokens.length);
				}
			}
			return devices;
		} catch (Exception e) {
			throw new CubeException("Failed to list usb devices", e);
		}
	}
}
