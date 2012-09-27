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
package ch.admin.vbs.cube.client.wm.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Display;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.WindowByReference;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XEvent;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XTextProperty;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class SniffX {
	private static final Logger LOG = LoggerFactory.getLogger(SniffX.class);

	public static void main(String[] args) {
		new SniffX().run();
		System.out.println("done");
	}

	private String displayName;
	private Display display;
	private X11 x11;
	private Thread eventThread;

	private void run() {
		x11 = X11.INSTANCE;
		displayName = ":0.0";
		display = x11.XOpenDisplay(displayName);
		//
		eventThread = new Thread(new Runnable() {
			private boolean destroyed;

			@Override
			public void run() {
				try {
					XEvent event = new XEvent();
					while (!destroyed) {
						try {
							// check for new events
							while (x11.XPending(display) > 0) {
								x11.XNextEvent(display, event);
								processEvent(event);
							}
							Thread.sleep(10);
						} catch (Exception e) {
							LOG.error("Error during catching events", e);
						}
					}
					x11.XCloseDisplay(display);
					x11.XCloseDisplay(display);
				} catch (Exception e) {
					LOG.error("XEvent Thread Failed");
				}
			}
		});
		eventThread.start();
		//
		Window rootWindow = x11.XRootWindow(display, 0);
		long[] childrenWindowIdArray = getChildrenList(display, rootWindow);
		for (long windowId : childrenWindowIdArray) {
			Window window = new Window(windowId);
			String name = getWindowName(window);
			if (name !=null && name.equals("VirtualBox")) {
				LOG.debug("Regitser events for [{}]",name);
				x11.XSelectInput(display, window, new NativeLong(X11.ResizeRedirectMask | X11.EnterWindowMask | X11.SubstructureNotifyMask
						| X11.StructureNotifyMask));
			}
		}
		x11.XCloseDisplay(display);
	}

	private long[] getChildrenList(Display display, Window parentWindow) {
		long[] childrenWindowIdArray = new long[] {};
		// prepare reference values
		WindowByReference rootWindowRef = new WindowByReference();
		WindowByReference parentWindowRef = new WindowByReference();
		PointerByReference childrenPtr = new PointerByReference();
		IntByReference childrenCount = new IntByReference();
		// find all children to the rootWindow
		if (x11.XQueryTree(display, parentWindow, rootWindowRef, parentWindowRef, childrenPtr, childrenCount) == 0) {
			LOG.error("BadWindow - A value for a Window argument does not name a defined Window!");
			return childrenWindowIdArray;
		}
		// get all window id's from the pointer and the count
		if (childrenCount.getValue() > 0) {
			childrenWindowIdArray = childrenPtr.getValue().getLongArray(0, childrenCount.getValue());
		}
		return childrenWindowIdArray;
	}

	public String getWindowName(Window w) {
		if (w == null)
			return null;
		XTextProperty windowTitle = new XTextProperty();
		x11.XFetchName(display, w, windowTitle);
		//
		//
		// //
		// X11.EWMH x = new X11.EWMH(display);
		// String s = x.readStringProperty(w, x11.XInternAtom(display,
		// "WM_NAME", false));
		//
		return windowTitle.value;
	}

	private void processEvent(XEvent event) {
		LOG.debug("Got an XEvent [{}]", X11.XEventName.getEventName(event.type));
		switch (event.type) {
		case X11.ResizeRequest: {
			X11.XResizeRequestEvent e = (X11.XResizeRequestEvent) event.getTypedValue(X11.XResizeRequestEvent.class);
			LOG.debug(String.format("XResizeRequestEvent (%s) (%dx%d) -> ignored", getWindowName(e.window), e.width, e.height));
		}
			break;
		case X11.PropertyNotify: {
			// we catch PropertyNotify to identify VM's window as soon as
			// its title is set and re-parent it in a border window
			X11.XPropertyEvent xe = (X11.XPropertyEvent) event.getTypedValue(X11.XPropertyEvent.class);
			String atomName = x11.XGetAtomName(display, xe.atom);
			if ("WM_NAME".equals(atomName)) {
				LOG.debug("Window name changed [{}]", getWindowName(xe.window));
			}
		}
			break;
		case X11.ConfigureNotify: {
			// we catch ConfigureNotify to enforce the desired frame size on
			// VM's windows.
			X11.XConfigureEvent e = (X11.XConfigureEvent) event.getTypedValue(X11.XConfigureEvent.class);
			LOG.debug(String.format("ConfigureNotify for window [%s] (%d:%d)(%dx%d)", e.window, e.x, e.y, e.width, e.height));
			break;
		}
		default:
			break;
		}
		x11.XFlush(display);
	}
}
