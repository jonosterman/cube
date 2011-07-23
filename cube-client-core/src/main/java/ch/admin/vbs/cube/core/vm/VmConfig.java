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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.common.container.Container;

public class VmConfig {
	private static final String DEFAULT_CONFIG_NAME = "cube-vm-config.xml";
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(VmConfig.class);
	private Properties properites = new Properties();
	private final Container vmContainer;
	private final Container rtContainer;

	public VmConfig(Container vmContainer, Container rtContainer) {
		this.vmContainer = vmContainer;
		this.rtContainer = rtContainer;
	}

	public void load() throws CubeException {
		try {
			if (rtContainer == null || rtContainer.getMountpoint() == null || !rtContainer.getMountpoint().exists()) {
				throw new IOException("invalid container mountpoint.");
			}
			File cfgFile = new File(rtContainer.getMountpoint(), DEFAULT_CONFIG_NAME);
			properites = new Properties();
			if (cfgFile.exists()) {
				FileInputStream fis = new FileInputStream(cfgFile);
				properites.loadFromXML(fis);
				fis.close();
			} else {
				LOG.warn("Missing config file [{}]", cfgFile.getAbsolutePath());
			}
		} catch (Exception e) {
			throw new CubeException("Failed to load vm config.", e);
		}
	}

	public void save() throws CubeException {
		try {
			if (rtContainer == null || rtContainer.getMountpoint() == null || !rtContainer.getMountpoint().exists()) {
				throw new IOException("invalid container mountpoint.");
			}
			File cfgFile = new File(rtContainer.getMountpoint(), DEFAULT_CONFIG_NAME);
			FileOutputStream fos = new FileOutputStream(cfgFile);
			properites.storeToXML(fos, "Cube VM Configuration File");
			fos.close();
		} catch (Exception e) {
			throw new CubeException("Failed to save vm config.", e);
		}
	}

	public Properties getProperties() {
		return properites;
	}

	public Container getVmContainer() {
		return vmContainer;
	}

	public Container getRtContainer() {
		return rtContainer;
	}
}
