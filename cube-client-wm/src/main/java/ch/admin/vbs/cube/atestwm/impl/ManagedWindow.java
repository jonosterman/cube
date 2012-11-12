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
package ch.admin.vbs.cube.atestwm.impl;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

/**
 * This class is only there to hold information about managed windows.
 */
public class ManagedWindow {
	public enum WindowType {
		VM, TABS, DIALOG, BG, OTHER
	}

	private Window client;
	private Window border;
	private WindowType type;
	// screen id is used for BG managed window
	private String screenId;

	public ManagedWindow(Window client, Window border, WindowType type) {
		this.client = client;
		this.border = border;
		this.type = type;
	}
	
//	public static final WindowType guessType(String windowName) {
//		if (windowName == null) {
//			return WindowType.OTHER;			
//		} else if(windowName.startsWith(TabManager.TABSFRAME_PREFIX)) {
//			return WindowType.TABS;
//		} else {
//			return WindowType.OTHER;
//		}
//	}
//	
//	
	// ######################

	public Window getClient() {
		return client;
	}

	public Window getBorder() {
		return border;
	}

	public WindowType getType() {
		return type;
	}

	public String getScreenId() {
		return screenId;
	}

	public void setXRScreenId(String screenId) {
		this.screenId = screenId;
	}
}
