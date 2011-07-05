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

package ch.admin.vbs.cube.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is used as basis to read configuration files.<br>
 * 
 * Earlier, there was only one configuration file which led to have a mix of
 * server and client settings at the same place. But now each project should
 * have its own configuration file. <br>
 * 
 * This class will load the configuration file in this priority order <br>
 * - look configuration file specified via VM parameter<br>
 * - look configuration file used for test<br>
 * - look configuration file used for production<br>
 * 
 * This way, the test configuration file will be used if present in classpath
 * instead of the production one.<br>
 * 
 * getInternProperty and setInternProperty are protected. The subclasses should
 * provide static setProperty and getProperty methods that call
 * getInternProperty and setInternProperty. <br>
 * 
 */
public abstract class AbstractProperties {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(AbstractProperties.class);
	private Properties properties;
	private final String propertyName;
	private final String prodFileName;
	private final String testFileName;

	/**
	 * Private constructor for convenience.
	 */
	protected AbstractProperties(String propertyName, String prodFileName, String testFileName) {
		this.propertyName = propertyName;
		this.prodFileName = prodFileName;
		this.testFileName = testFileName;
		init();
	}

	private void init() {
		properties = new Properties();
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			// 1) Use file specified in VM properties
			URL url = null;
			if (System.getProperty(propertyName) != null) {
				LOG.debug("Use configuration file specified via system property.");
				url = new File(System.getProperty(propertyName)).toURI().toURL();
			}
			// 2) Fallback, using test configuration file (will be present in
			// classpath during tests)
			if (url == null) {
				// fallback to test config if present (typically allong with junit)
				url = cl.getResource(testFileName);
			}
			// 3) Fallback, using 'standard' configuration file
			if (url == null) {
				url = cl.getResource(prodFileName);
			}
			// load properties file
			if (url == null) {
				LOG.error("no configuration file found [{}]. Use property [{}] to specify a custom file.", testFileName + " / " + prodFileName, propertyName);
			} else {
				// print debug and configuration hint
				LOG.debug("Load properties file [{}]. Use property [{}] to specify a custom file.", url.toString(), propertyName);
				InputStream is = url.openStream();
				properties.loadFromXML(is);
				is.close();
			}
			// dump properties (debug)
			// for (Entry e : properties.entrySet()) {
			// LOG.info("$> [{}] = [{}]", e.getKey(), e.getValue());
			// }
		} catch (IOException e) {
			LOG.error("Cannot load properties file", e);
			throw new RuntimeException("Cannot load properties file", e);
		}
		// override all properties with system properties. It allows to override
		// any configuration parameter through command line.
		for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
			properties.setProperty(entry.getKey().toString(), entry.getValue().toString());
		}
	}

	/**
	 * Searches for the property with the specified key in this property list.
	 * 
	 * @param key
	 *            - the property key.
	 * @return the value in this property list with the specified key value.
	 */
	protected String getInternProperty(String key) {
		return properties.getProperty(key);
	}

	protected void setInternProperty(String key, String value) {
		properties.setProperty(key, value);
	}
}
