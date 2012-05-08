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

package ch.admin.vbs.cube.client.wm.ui.x.imp;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.ui.x.IWindowManagerCallback;
import ch.admin.vbs.cube.client.wm.ui.x.IXWindowManager;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Display;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.WindowByReference;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XConfigureEvent;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XEvent;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XTextProperty;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XWindowAttributes;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * The XWindowManager is a singleton implementation, that encapsulate the x11
 * calls from the xlib.
 */
public final class XWindowManager2 implements IXWindowManager {
	private enum Arch {
		OS_32_BIT, OS_64_BIT
	}

	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(XWindowManager2.class);
	private static final int CHECKING_INTERVAL_FOR_NEW_EVENTS = 10;
	private Arch osArch;
	private String displayName = null;
	private int screenIndex = 0;
	private Thread eventThread = null;
	private boolean destroyed = false;
	private X11 x11;
	/**
	 * a Display which must be held open during the whole life time, so that the
	 * registered events can be caught.
	 */
	private Display eventDisplay;
	private Display display;
	private IWindowManagerCallback cb;

	public void start() {
		/*
		 * detect OS architecture since it plays a role in some X calls (window
		 * pointer size)
		 */
		String osBits = System.getProperty("os.arch");
		LOG.debug("Detected architecture [{}]", osBits);
		if ("amd64".equals(osBits)) {
			osArch = Arch.OS_64_BIT;
		} else {
			osArch = Arch.OS_32_BIT;
		}
		/*
		 * Detect and set the display name and the screen index. Default is
		 * ":0.0" for display name and 0 for the screen index.
		 */
		displayName = System.getenv("DISPLAY");
		if (displayName != null) {
			// displayName is something like this ":0.0"
			int dotIndex = displayName.indexOf(".");
			if (dotIndex < 0) {
				screenIndex = 0;
			} else {
				screenIndex = Integer.parseInt(displayName.substring(dotIndex + 1));
			}
		} else {
			displayName = ":0.0";
			screenIndex = 0;
		}
		// get Xlib reference and make it somewhat thread safe
		x11 = X11.INSTANCE;
		x11.XInitThreads();
		/*
		 * register root window's events to be notified when a new window pops
		 * up
		 */
		eventDisplay = x11.XOpenDisplay(displayName);
		display = eventDisplay;// x11.XOpenDisplay(displayName);
		x11.XSelectInput(eventDisplay, x11.XRootWindow(eventDisplay, screenIndex), new NativeLong(X11.SubstructureNotifyMask | X11.PropertyChangeMask
				| X11.StructureNotifyMask));
		/* start event thread */
		eventThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					XEvent event = new XEvent();
					while (!destroyed) {
						try {
							// check for new events
							while (x11.XPending(eventDisplay) > 0) {
								x11.XNextEvent(eventDisplay, event);
								processEvent(event);
							}
							Thread.sleep(CHECKING_INTERVAL_FOR_NEW_EVENTS);
						} catch (Exception e) {
							LOG.error("Error during catching events", e);
						}
					}
					x11.XCloseDisplay(eventDisplay);
					x11.XCloseDisplay(display);
				} catch (Exception e) {
					LOG.error("XEvent Thread Failed");
				}
			}
		});
		eventThread.start();
	}

	@Override
	public void reparentClientWindow(Window border, Window client, Rectangle clientBounds) {
		// Events to consider for client window :
		// - ResizeRedirectMask: to avoid VM to resize itself
		// - EnterWindowMask: to force focus on current VM
		// - SubstructureNotifyMask: to get ConfigureNotify events
		// - StructureNotifyMask: to get ConfigureNotify events
		x11.XSelectInput(eventDisplay, client, new NativeLong(X11.ResizeRedirectMask | X11.EnterWindowMask | X11.SubstructureNotifyMask
				| X11.StructureNotifyMask));
		// re-parent the x window and unmap it
		// Display display = x11.XOpenDisplay(displayName);
		// Unmap client window (will be mapped only when explicitly displayed)
		x11.XUnmapWindow(display, client);
		x11.XFlush(display);
		// Re-order the virtual machine window as child to the border window
		int result = x11.XReparentWindow(display, client, border, 0, 0);
		x11.XMapWindow(display, client);
		// set save set or the window will be closed after calling
		x11.XChangeSaveSet(display, client, X11.SetModeInsert);
		// resize
		x11.XMoveResizeWindow(display, client, clientBounds.x, clientBounds.y, clientBounds.width, clientBounds.height);
		//
		x11.XFlush(display);
		// x11.XCloseDisplay(display);
		LOG.debug(String.format("reparentWindow() [%d] - unmapped client[%s / %s] as child of border[%s]\n", result, getWindowName(client), client, border));
	}

	@Override
	public synchronized Window findWindowByTitle(String name) {
		/**
		 * Look in the root window if there is a window with this name
		 */
		Window foundWindow = null;
		// Display display = x11.XOpenDisplay(displayName);
		// get the root window
		Window rootWindow = x11.XRootWindow(display, screenIndex);
		long[] childrenWindowIdArray = getChildrenList(display, rootWindow);
		for (long windowId : childrenWindowIdArray) {
			Window window = new Window(windowId);
			// get window attributes
			XWindowAttributes attributes = new XWindowAttributes();
			x11.XGetWindowAttributes(display, window, attributes);
			// get window title
			XTextProperty windowTitle = new XTextProperty();
			x11.XFetchName(display, window, windowTitle);
			// filter windows with attributes which our windows do not have
			if (!attributes.override_redirect && windowTitle.value != null) {
				// LOG.debug("Scan windows [{}] [{}]",windowTitle.value,name);
				if (name.equals(windowTitle.value)) {
					foundWindow = window;
					break;
				}
			}
		}
		// close display
		// x11.XCloseDisplay(display);
		x11.XFlush(display);
		if (foundWindow == null) {
			LOG.error("No XWindow found that match name [{}]", name);
		}
		return foundWindow;
	}

	@Override
	public synchronized void showOnlyTheseWindow(Collection<Window> hideWindowList, Collection<Window> showWindowList) {
		LOG.debug("showOnlyTheseWindow()");
		// connection to the x server)
		// Display display = x11.XOpenDisplay(displayName);
		if (showWindowList != null) {
			// maps and sets all show window
			for (Window window : showWindowList) {
				LOG.debug("Raise window {}", window);
				// map window
				x11.XMapWindow(display, window);
				x11.XMapRaised(display, window);
			}
		}
		if (hideWindowList != null) {
			// set all visible window hidden
			for (Window window : hideWindowList) {
				// get window attributes
				XWindowAttributes attributes = new XWindowAttributes();
				x11.XGetWindowAttributes(display, window, attributes);
				if (attributes.map_state != X11.IsUnmapped) {
					LOG.debug("Unmap window {}", window);
					x11.XUnmapWindow(display, window);
				}
			}
		}
		// commit changes and close display
		x11.XFlush(display);
		// x11.XCloseDisplay(display);
	}

	@Override
	public Window createBorderWindow(Window frame, int borderSize, Color borderColor, Color backgroundColor, Rectangle bounds) {
		/*
		 * connection to the x server and set resources permanent, otherwise
		 * window would be destroyed by calling XCloseDisplay
		 */
		// Display display = x11.XOpenDisplay(displayName);
		x11.XSetCloseDownMode(display, X11.RetainPermanent);
		/*
		 * Create window which will hold the virtual machine window and paints a
		 * border. This border window is a child of the parent window.
		 */
		Window borderWindow = x11.XCreateSimpleWindow(//
				display, //
				frame, //
				bounds.x, //
				bounds.y, //
				bounds.width, //
				bounds.height, //
				borderSize, //
				borderColor.getRGB(), //
				backgroundColor.getRGB());
		LOG.debug("Bordered window created [{}]", borderWindow);
		// close display
		// x11.XCloseDisplay(display);
		return borderWindow;
	}

	@Override
	public void removeWindow(Window window) {
		LOG.debug("Remove Window [{}]", window);
		// Display display = x11.XOpenDisplay(displayName);
		x11.XDestroyWindow(display, window);
		// commit changes and close display
		x11.XFlush(display);
		// x11.XCloseDisplay(display);
	}

	@Override
	public void hideAndReparentToRoot(Window window) {
		// Display display = x11.XOpenDisplay(displayName);
		// Map the virtual machine window as child to the border window
		x11.XUnmapWindow(display, window);
		Window rootWindow = x11.XRootWindow(display, screenIndex);
		x11.XReparentWindow(display, window, rootWindow, 0, 0);
		// commit changes and close display
		x11.XFlush(display);
		// x11.XCloseDisplay(display);
		LOG.debug("unmap and reparentWindow() - child[{} / {}]  to root\n", getWindowName(window), window);
	}

	@Override
	public void reparentWindowAndResize(Window frame, Window border, Rectangle bounds, Window client, Rectangle clientBounds) {
		// (used when moving a VM from a monitor to another one)
		LOG.debug("Reparent window [{}] to parent [{}]", border, frame);
		// Display display = x11.XOpenDisplay(displayName);
		// move border window to new CubeFrame
		x11.XUnmapWindow(display, border);
		x11.XUnmapWindow(display, client);
		x11.XFlush(display);
		x11.XReparentWindow(display, border, frame, bounds.x, bounds.y);
		// resize border window to the new CubeFrame size (X does not include
		// border in width)
		x11.XMoveResizeWindow(display, border, bounds.x, bounds.y, bounds.width, bounds.height);
		x11.XMoveResizeWindow(display, client, clientBounds.x, clientBounds.y, clientBounds.width, clientBounds.height);
		x11.XMapWindow(display, client);
		x11.XMapWindow(display, border);
		x11.XFlush(display);
		// close display
		// sendResizeEvent(display, border, outerBounds);
		// x11.XCloseDisplay(display);
	}

	@Override
	public synchronized void destroy() {
		if (!destroyed) {
			destroyed = true;
		}
	}

	/**
	 * Processes events which has been registered.
	 * 
	 * @param event
	 *            the thrown event
	 * @see XWindowManager2#registerWindowForEvents(Window)
	 */
	private void processEvent(XEvent event) {
		LOG.debug("Got an XEvent [{}]", X11.XEventName.getEventName(event.type));
		if (!destroyed) {
			switch (event.type) {
			case X11.ResizeRequest: {
				X11.XResizeRequestEvent e = (X11.XResizeRequestEvent) event.getTypedValue(X11.XResizeRequestEvent.class);
				// this events are sent from VM's window. Does not process them
				// ----------------
				// Completely hacked XMoveWindow() in order to
				// VirtualBox to trigger guest resize..
				Rectangle bnd = cb.getPreferedClientBounds(e.window);
				if (bnd != null) {
					x11.XMoveWindow(display, e.window, 1, 1);
					x11.XFlush(display);
				}
				// ----------------
				// Rectangle bnd = cb.getPreferedClientBounds(e.window);
				// if (bnd != null) {
				// LOG.debug(String.format("XResizeRequestEvent (%s) (%dx%d) -> executed(%dx%d)",
				// getWindowName(e.window), e.width, e.height, bnd.width,
				// bnd.height));
				// Display display = x11.XOpenDisplay(displayName);
				// x11.XResizeWindow(display, e.window, bnd.width, bnd.height);
				// sendResizeEvent(display, e.window, bnd);
				// x11.XCloseDisplay(display);
				// } else {
				LOG.debug(String.format("XResizeRequestEvent (%s) (%dx%d) -> ignored", getWindowName(e.window), e.width, e.height));
				// }
			}
				break;
			case X11.EnterNotify:
				// we catch EnterNotify to ensure that the visible VM will get
				// the keyboard focus.
				X11.XCrossingEvent enterWindowEvent = (X11.XCrossingEvent) event.getTypedValue(X11.XCrossingEvent.class);
				x11.XSetInputFocus(eventDisplay, enterWindowEvent.window, X11.RevertToParent, new NativeLong(0));
				break;
			case X11.PropertyNotify: {
				// we catch PropertyNotify to identify VM's window as soon as
				// its title is set and re-parent it in a border window
				X11.XPropertyEvent xe = (X11.XPropertyEvent) event.getTypedValue(X11.XPropertyEvent.class);
				String atomName = x11.XGetAtomName(eventDisplay, xe.atom);
				if ("WM_NAME".equals(atomName)) {
					LOG.debug("Window name changed [{}]", getWindowName(xe.window));
					notifyWindowTitleChange(xe.window);
				}
			}
				break;
			case X11.CreateNotify: {
				// we catch CreateNotify to register an event listener to get
				// PropertyNotify for this window (see above)
				X11.XCreateWindowEvent xe = (X11.XCreateWindowEvent) event.getTypedValue(X11.XCreateWindowEvent.class);
				x11.XSelectInput(eventDisplay, xe.window, new NativeLong(X11.PropertyChangeMask));
				x11.XFlush(eventDisplay);
			}
				break;
			case X11.DestroyNotify: {
				// we catch DestroyNotify : to update the ManagedWindow model in
				// WindowManager.
				X11.XDestroyWindowEvent xe = (X11.XDestroyWindowEvent) event.getTypedValue(X11.XDestroyWindowEvent.class);
				notifyWindowDestroyed(xe.window);
			}
				break;
			// case X11.MapNotify: {
			// X11.XMapEvent e = (X11.XMapEvent)
			// event.getTypedValue(X11.XMapEvent.class);
			// Display display = x11.XOpenDisplay(displayName);
			// // Completely hacked XMoveWindow() in order to
			// // VirtualBox to trigger guest resize..
			// x11.XMoveWindow(display, e.window, 1, 1);
			// x11.XFlush(display);
			// //
			// x11.XCloseDisplay(display);
			// }
			// break;
			case X11.ConfigureNotify: {
				// we catch ConfigureNotify to enforce the desired frame size on
				// VM's windows.
				X11.XConfigureEvent e = (X11.XConfigureEvent) event.getTypedValue(X11.XConfigureEvent.class);
				if (e.window == null) {
					LOG.debug(String.format("ConfigureNotify for unmanaged window [%s] (%d:%d)(%dx%d)", e.window, e.x, e.y, e.width, e.height));
				} else {
					Rectangle prefBnds = cb.getPreferedClientBounds(e.window);
					if (prefBnds == null) {
						LOG.debug(String.format("ConfigureNotify for unmanaged window [%s/%s] (%d:%d)(%dx%d)", e.window, getWindowName(e.window), e.x, e.y,
								e.width, e.height));
					} else {
						LOG.debug(String.format("ConfigureNotify for managed window [%s] (%d:%d)(%dx%d)", getWindowName(e.window), e.x, e.y, e.width, e.height));
						// check client
						// if (e.width != prefBnds.width || e.height !=
						// prefBnds.getHeight()) {
						// LOG.debug("Force client resizing ({}x{})",
						// prefBnds.width, prefBnds.height);
						// // force resize
						// x11.XMoveResizeWindow(display, e.window, prefBnds.x,
						// prefBnds.y, prefBnds.width, prefBnds.height);
						// }
						if (e.x != prefBnds.x || e.y != prefBnds.x) {
							LOG.debug("Force client moving ({}:{})", prefBnds.x, prefBnds.y);
							// force move at the right place
							x11.XMoveWindow(display, e.window, prefBnds.x, prefBnds.y);
						}
					}
				}
			}
				break;
			default:
				break;
			}
		}
		x11.XFlush(display);
	}

	private void notifyWindowTitleChange(Window window) {
		cb.windowTitleUpdated(window, getWindowName(window));
	}

	private void notifyWindowDestroyed(Window window) {
		cb.windowDestroyed(window);
	}

	@Override
	public String getWindowName(Window w) {
		if (w == null)
			return null;
		XTextProperty windowTitle = new XTextProperty();
		x11.XFetchName(eventDisplay, w, windowTitle);
		return windowTitle.value;
	}

	@Override
	public void setWindowManagerCallBack(IWindowManagerCallback cb) {
		this.cb = cb;
	}

	/**
	 * Returns all window IDs to the given parent window.
	 * 
	 * @param display
	 *            the display for better performance
	 * @param parentWindow
	 *            the parent window for all children
	 * @return a list of long which are window IDs
	 */
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
			switch (osArch) {
			case OS_32_BIT:
				int[] intChildrenWindowIdArray = childrenPtr.getValue().getIntArray(0, childrenCount.getValue());
				childrenWindowIdArray = new long[intChildrenWindowIdArray.length];
				int index = 0;
				for (int windowId : intChildrenWindowIdArray) {
					childrenWindowIdArray[index] = windowId;
					++index;
				}
				break;
			case OS_64_BIT:
				childrenWindowIdArray = childrenPtr.getValue().getLongArray(0, childrenCount.getValue());
				break;
			}
		}
		return childrenWindowIdArray;
	}

	public void resize(Window client, int w, int h) {
		// Display display = x11.XOpenDisplay(displayName);
		LOG.debug("RESIZE [{}]", getWindowName(client));
		x11.XResizeWindow(display, client, w, h);
		x11.XFlush(display);
		// x11.XCloseDisplay(display);
	}

	public void moveResize(Window client, int x, int y, int w, int h) {
		// Display display = x11.XOpenDisplay(displayName);
		LOG.debug("MOVE+RESIZE [{}]", getWindowName(client));
		x11.XMoveResizeWindow(display, client, x, y, w, h);
		x11.XFlush(display);
		// x11.XCloseDisplay(display);
	}

	public void move(Window client, int x, int y) {
		// Display display = x11.XOpenDisplay(displayName);
		LOG.debug("MOVE [{}]", getWindowName(client));
		x11.XMoveWindow(display, client, x, y);
		x11.XFlush(display);
		// x11.XCloseDisplay(display);
	}

	private void sendResizeEvent(Display display, Window client, Rectangle bounds) {
		LOG.debug("---> sendResizeEvent() to [{}] name[{}]", client, getWindowName(client));
		// Send an event to force application (VirtualBox) to resize. It is
		// needed if we move the VM on a 2nd screen with another resolution
		// than the first one.
		NativeLong event_mask = new NativeLong(X11.ConfigureNotify);
		XConfigureEvent event = new XConfigureEvent();
		event.type = X11.ConfigureNotify;
		event.display = display;
		event.height = bounds.height;
		event.width = bounds.width;
		event.border_width = 0;
		event.above = null;
		event.override_redirect = 0;
		x11.XSendEvent(display, client, 1, event_mask, event);
	}
}
