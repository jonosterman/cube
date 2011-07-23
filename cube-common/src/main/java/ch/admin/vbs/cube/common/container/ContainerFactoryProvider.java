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

package ch.admin.vbs.cube.common.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeCommonProperties;
import ch.admin.vbs.cube.common.CubeException;

/**
 * Initialize the right container factory based on the configuration file.
 */
public class ContainerFactoryProvider {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ContainerFactoryProvider.class);
	private static IContainerFactory factory = null;

	public static IContainerFactory getFactory() throws CubeException {
		String classname = CubeCommonProperties.getProperty("cube.containerFactoryImpl");
		synchronized (ContainerFactoryProvider.class) {
			if (factory == null) {
				// create instance of container factory
				try {
					factory = (IContainerFactory) Class.forName(classname).newInstance();
					LOG.debug("Use container factory [{}].", factory.getClass());
				} catch (Exception e) {
					throw new CubeException("Failed to init container factory [" + classname + "]", e);
				}
			}
			return factory;
		}
	}
}
