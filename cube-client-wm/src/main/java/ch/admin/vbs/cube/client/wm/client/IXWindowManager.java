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

package ch.admin.vbs.cube.client.wm.client;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collection;

import ch.admin.vbs.cube.client.wm.ui.x.IWindowManagerCallback;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public interface IXWindowManager {
	/**
	 * Finds the x window with the matching name pattern for the window title
	 * asynchronesly. When the x window was found, it will be reparent to the
	 * binding x window and raised to the top.
	 * 
	 * @param vmId
	 *            vmId of the x window to be found
	 * @param namePattern
	 *            the unique name pattern to find the x window
	 * @param bindingWindow
	 *            the parent window for the found x window
	 */
	public abstract void findAndBindWindowByNamePattern(final String vmId, final String namePattern, final Window bindingWindow);

	/**
	 * Returns the x11 window by the given name (title).
	 * 
	 * @param name
	 *            the name/title of the window
	 * @return the x11 window instance or null when the window can not be found
	 */
	public abstract Window findWindowByNamePattern(String name);

	/**
	 * Hides all virtual machine windows in the hideWindowList and brings up all
	 * virtual machine window in the showWindowList. Other window will not be
	 * affected.
	 * 
	 * @param hideWindowList
	 *            the list of hidding windows
	 * @param showWindowList
	 *            the list of showing windows (normally just one)
	 */
	public abstract void showOnlyTheseWindow(Collection<Window> hideWindowList, Collection<Window> showWindowList);

	/**
	 * Creates a new x11 window with a border and binds it to the given parent.
	 * 
	 * @param parentWindow
	 *            the parent of the new x11 window
	 * @param borderSize
	 *            the size of the border
	 * @param borderColor
	 *            the color of the border
	 * @param backgroundColor
	 *            background of the new x11 window
	 * @param bounds
	 *            the bounds of the new x11 window with the border
	 * @return the new x11 window
	 */
	public abstract Window createBorderWindow(Window parentWindow, int borderSize, Color borderColor, Color backgroundColor, Rectangle bounds);

	/**
	 * Removes the window with all his child windows.
	 * 
	 * @param window
	 *            the window to be removed
	 */
	public abstract void removeWindow(Window window);

	/**
	 * Reparents a window and resizes it after.
	 * 
	 * @param parentWindow
	 *            the parent window of the reparent child
	 * @param childWindow
	 *            the child window to be reparent
	 * @param bounds
	 *            the bound to which the child should be resized and moved
	 */
	public abstract void reparentWindowAndResize(Window parentWindow, Window childWindow, Rectangle bounds);

	/**
	 * Destroys all thread by shutting down them.
	 */
	public abstract void destroy();

	/**
	 * Adjusts the children window of the parent window to the specific
	 * dimenstion.
	 * 
	 * @param parentWindow
	 *            the parent window
	 * @param width
	 *            the width for the children
	 * @param height
	 *            the height for the children
	 */
	public abstract void adjustScreenForChild(Window parentWindow, int width, int height);

	public abstract void setWindowManagerCallBack(IWindowManagerCallback cb);

	public abstract String getWindowName(Window w);


}