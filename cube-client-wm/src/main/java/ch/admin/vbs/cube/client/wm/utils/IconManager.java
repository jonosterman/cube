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

package ch.admin.vbs.cube.client.wm.utils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The icon manager is a singleton class and keeps all loaded icons in the
 * memory.
 * 
 * 
 */
public final class IconManager {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(IconManager.class);
	private static final String ICON_BASE_DIRECTORY = "themes/";
	private Map<String, ImageIcon> iconMap = null;
	private static IconManager instance = new IconManager();

	/**
	 * Creates the singleton instance of the icon manager.
	 */
	private IconManager() {
		iconMap = new HashMap<String, ImageIcon>();
	}

	/**
	 * Returns the singleton instance of the icon manager.
	 * 
	 * @return the singleton instance
	 */
	public static IconManager getInstance() {
		return instance;
	}

	/**
	 * Returns the image icon for the given name.
	 * 
	 * @param iconName
	 *            the icon name without the exact location (the icon must be in
	 *            the ICON_BASE_DIRECTORY)
	 * @return the icon for the given name or null if it could not be found
	 */
	public synchronized ImageIcon getIcon(String iconName) {
		ImageIcon icon = iconMap.get(iconName);
		if (icon == null) {
			String path = ICON_BASE_DIRECTORY + CubeClientWmProperties.getProperty("WindowManager.theme") + "/" + iconName;
			URL iconUrl = ClassLoader.getSystemResource(path);
			if (iconUrl != null) {
				icon = new ImageIcon(iconUrl);
			}
			if (icon == null) {
				if (LOG.isErrorEnabled()) {
					LOG.error("Icon with the path '" + path + "' couldn't be loaded!");
				}
			} else {
				iconMap.put(iconName, icon);
			}
		}
		return icon;
	}
}
