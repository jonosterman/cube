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

package ch.admin.vbs.cube.client.wm.mock;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.IXWindowManager;
import ch.admin.vbs.cube.client.wm.ui.x.IWindowManagerCallback;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public class MockXWindowManager implements IXWindowManager {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(MockXWindowManager.class);
	private Random d = new Random();
	
	@Override
	public void adjustScreenForChild(Window parentWindow, int width, int height) {
	}

	@Override
	public Window createBorderWindow(Window parentWindow, int borderSize, Color borderColor, Color backgroundColor,
			Rectangle bounds) {
		return null;
	}

	@Override
	public void destroy() {
		LOG.debug("not implemented");
	}

	@Override
	public void findAndBindWindowByNamePattern(String vmId, String namePattern, Window bindingWindow) {
		LOG.debug("not implemented");
	}

	@Override
	public Window findWindowByNamePattern(String name) {
		Window w = new Window(d.nextLong());
		return w;
	}

	@Override
	public void removeWindow(Window window) {
		LOG.debug("not implemented");
	}

	@Override
	public void reparentWindowAndResize(Window parentWindow, Window childWindow, Rectangle bounds) {
		LOG.debug("not implemented");
	}

	@Override
	public void showOnlyTheseWindow(Collection<Window> hideWindowList, Collection<Window> showWindowList) {
		LOG.debug("not implemented");
	}
	
	@Override
	public String getWindowName(Window w) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setWindowManagerCallBack(IWindowManagerCallback cb) {
		// TODO Auto-generated method stub
		
	}
	
}
