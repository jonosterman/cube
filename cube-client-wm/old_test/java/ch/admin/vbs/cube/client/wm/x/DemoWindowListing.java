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

package ch.admin.vbs.cube.client.wm.x;

import org.junit.Ignore;
import org.junit.Test;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Display;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.WindowByReference;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XTextProperty;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XWindowAttributes;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * List all X windows.
 */
public class DemoWindowListing {
	private X11 x11 = X11.INSTANCE;
	
	public static void main(String[] args) {
		DemoWindowListing twl = new DemoWindowListing();
		twl.testListing();
	}

	@Test
	@Ignore
	private void testListing() {
		Display display = x11.XOpenDisplay(":0");
		Window rootWindow = x11.XRootWindow(display, 0);
        long[] childrenWindowIdArray = getChildrenListArch64(display, rootWindow);
        for (long windowId : childrenWindowIdArray) {
            Window window = new Window(windowId);
            // get window attributes
            XWindowAttributes attributes = new XWindowAttributes();
            x11.XGetWindowAttributes(display, window, attributes);
            // get window title
            XTextProperty windowTitle = new XTextProperty();
            x11.XFetchName(display, window, windowTitle);

            System.out.printf("Scan windows [%s]\n",windowTitle.value);
        }
	}
	
	
	private long[] getChildrenListArch64(Display display, Window parentWindow) {
        long[] childrenWindowIdArray = new long[] {};

        // prepare reference values
        WindowByReference rootWindowRef = new WindowByReference();
        WindowByReference parentWindowRef = new WindowByReference();
        PointerByReference childrenPtr = new PointerByReference();
        IntByReference childrenCount = new IntByReference();

        // find all children to the rootWindow
        if (x11.XQueryTree(display, parentWindow, rootWindowRef, parentWindowRef, childrenPtr, childrenCount) == 0) {
            x11.XCloseDisplay(display);
            return childrenWindowIdArray;
        }

        // get all window id's from the pointer and the count
        if (childrenCount.getValue() > 0) {
            childrenWindowIdArray = childrenPtr.getValue().getLongArray(0, childrenCount.getValue());
        }

        return childrenWindowIdArray;
	}
}
