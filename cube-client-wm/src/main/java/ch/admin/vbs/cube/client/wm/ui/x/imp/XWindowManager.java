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

import ch.admin.vbs.cube.client.wm.client.IXWindowManager;
import ch.admin.vbs.cube.client.wm.ui.x.IWindowManagerCallback;
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
public final class XWindowManager implements IXWindowManager {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(XWindowManager.class);
	private static final int WAITING_TIME = 100;
	private static final int FINDING_WINDOW_TIMEOUT = 2000;
	private static final int CHECKING_INTERVAL_FOR_NEW_EVENTS = 10;
	private static final int OS_32_BIT = 32;
	private static final int OS_64_BIT = 64;
	private int osArchitectur = 0;
	private String displayName = null;
	private int screenIndex = 0;
	private static XWindowManager instance;
	private Thread eventThread = null;
	private boolean destroyed = false;
	private X11 x11;
	/**
	 * Display which must be held open during the whole life time, so that the
	 * registered events can be caught. Otherwise all registered events will be
	 * lost.
	 */
	private Display eventDisplay;
	private IWindowManagerCallback cb;

	/**
	 * Creates the x window manager singleton instance.
	 */
	private XWindowManager() {
	}

	public void start() {
		detectOperationSystemArchitecture();
		detectDefaultDisplay();
		x11 = X11.INSTANCE;
		x11.XInitThreads();
		eventDisplay = x11.XOpenDisplay(displayName);
		// Event processor
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
				} catch (Exception e) {
					LOG.error("XEvent Thread Failed");
				}
			}
		});
		eventThread.start();
		//
		registerRootWindowEvents();
	}

	/**
	 * Returns the singleton x window manager instance.
	 * 
	 * @return the x window manager instance
	 */
	public static synchronized IXWindowManager getInstance() {
		if (instance == null) {
			instance = new XWindowManager();
		} else if (instance.destroyed) {
			instance = new XWindowManager();
		}
		return instance;
	}

	/**
	 * Detectes and sets the current operation system architecture. Default is
	 * 32Bit.
	 */
	private void detectOperationSystemArchitecture() {
		String osBits = System.getProperty("os.arch");
		LOG.debug("Detected architecture [{}]", osBits);
		if ("amd64".equals(osBits)) {
			osArchitectur = OS_64_BIT;
		} else {
			osArchitectur = OS_32_BIT;
		}
	}

	/**
	 * Detectes and sets the display name and the screen index. Default is
	 * ":0.0" for display name and 0 for the screen index.
	 */
	private void detectDefaultDisplay() {
		// get display name from system environment
		displayName = System.getenv("DISPLAY");
		if (LOG.isInfoEnabled()) {
			LOG.info("Display name is '" + displayName + "'.");
		}
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
		if (LOG.isInfoEnabled()) {
			LOG.info("Display name is '" + displayName + "' and screen index is '" + screenIndex + "'.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.cube.client.wm.x.XXWindowManager#findAndBindWindowByNamePattern
	 * (java.lang.String, java.lang.String,
	 * ch.admin.vbs.cube.client.wm.x.X11.Window)
	 */
	public void findAndBindWindowByNamePattern(final String vmId, final String namePattern, final Window bindingWindow) {
		// Starts a thread that will look for a windows with a title that match
		// the given one (actually a VirtualBox Window with a specific UUID).
		// Loop until this window has been found or FINDING_WINDOW_TIMEOUT has
		// been reached.
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				int timeout = 0;
				Window insideWindow = null; // will point on the found window
				while (insideWindow == null && timeout < FINDING_WINDOW_TIMEOUT) {
					// search window
					insideWindow = findWindowByNamePattern(namePattern);
					// if no matching window has been found: wait and try again.
					if (insideWindow == null) {
						try {
							Thread.sleep(WAITING_TIME);
							timeout += WAITING_TIME;
							if (LOG.isDebugEnabled()) {
								LOG.debug("Looking for window {" + namePattern + "} ");
							}
						} catch (InterruptedException e) {
							break;
						}
					}
				}
				if (insideWindow != null) {
					// It founds a matching window
					if (LOG.isDebugEnabled()) {
						LOG.debug("Window with namePattern='" + namePattern + "' found");
					}
					// register window for events
					registerWindowForEvents(insideWindow);
					// re-parent the x window and show this window alone
					reparentWindow(bindingWindow, insideWindow, 0, 0);
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Window with namePattern='" + namePattern + "' not found");
					}
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.cube.client.wm.x.XXWindowManager#findWindowByNamePattern
	 * (java.lang.String)
	 */
	public synchronized Window findWindowByNamePattern(String name) {
		Window foundWindow = null;
		Display display = x11.XOpenDisplay(displayName);
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
				if (windowTitle.value.matches(name)) {
					foundWindow = window;
					break;
				}
			}
		}
		// close display
		x11.XCloseDisplay(display);
		return foundWindow;
	}

	private synchronized Window findRootWindow() {
		Display display = x11.XOpenDisplay(displayName);
		// get the root window
		Window rootWindow = x11.XRootWindow(display, screenIndex);
		x11.XCloseDisplay(display);
		return rootWindow;
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
			// close display
			x11.XCloseDisplay(display);
			return childrenWindowIdArray;
		}
		// get all window id's from the pointer and the count
		if (childrenCount.getValue() > 0) {
			if (osArchitectur == OS_32_BIT) {
				int[] intChildrenWindowIdArray = childrenPtr.getValue().getIntArray(0, childrenCount.getValue());
				childrenWindowIdArray = new long[intChildrenWindowIdArray.length];
				int index = 0;
				for (int windowId : intChildrenWindowIdArray) {
					childrenWindowIdArray[index] = windowId;
					++index;
				}
			} else if (osArchitectur == OS_64_BIT) {
				childrenWindowIdArray = childrenPtr.getValue().getLongArray(0, childrenCount.getValue());
			} else {
				if (LOG.isInfoEnabled()) {
					LOG.warn("OS architecture is not supported or could not be mapped! Trying 32 bit os architecture.");
				}
				int[] intChildrenWindowIdArray = childrenPtr.getValue().getIntArray(0, childrenCount.getValue());
				childrenWindowIdArray = new long[intChildrenWindowIdArray.length];
				int index = 0;
				for (int windowId : intChildrenWindowIdArray) {
					childrenWindowIdArray[index] = windowId;
					++index;
				}
			}
		}
		return childrenWindowIdArray;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.cube.client.wm.x.XXWindowManager#showOnlyTheseWindow(java
	 * .util.Collection, java.util.Collection)
	 */
	public synchronized void showOnlyTheseWindow(Collection<Window> hideWindowList, Collection<Window> showWindowList) {
		// connection to the x server)
		Display display = x11.XOpenDisplay(displayName);
		if (hideWindowList != null) {
			// set all visible window hidden
			for (Window window : hideWindowList) {
				// get window attributes
				XWindowAttributes attributes = new XWindowAttributes();
				x11.XGetWindowAttributes(display, window, attributes);
				if (attributes.map_state != X11.IsUnmapped) {
					x11.XUnmapWindow(display, window);
				}
			}
		}
		if (showWindowList != null) {
			// maps and sets all show window
			for (Window window : showWindowList) {
				x11.XMapWindow(display, window);
				x11.XMapRaised(display, window);
			}
		}
		// commit changes and close display
		x11.XFlush(display);
		x11.XCloseDisplay(display);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.cube.client.wm.x.XXWindowManager#createBorderWindow(ch.admin
	 * .vbs.cube.client.wm.x.X11.Window, int, java.awt.Color, java.awt.Color,
	 * java.awt.Rectangle)
	 */
	public Window createBorderWindow(Window parentWindow, int borderSize, Color borderColor, Color backgroundColor, Rectangle bounds) {
		// connection to the x server and set resources permanent, otherwise
		// window would be destroyed by calling
		// XCloseDisplay
		Display display = x11.XOpenDisplay(displayName);
		x11.XSetCloseDownMode(display, X11.RetainPermanent);
		// Create window which holds the virtual machine window and paints a
		// border. This border window is a child of
		// the parent window.
		Window borderWindow = x11.XCreateSimpleWindow(display, parentWindow, bounds.x, bounds.y, bounds.width - 2 * borderSize, bounds.height - 2 * borderSize,
				borderSize, borderColor.getRGB(), backgroundColor.getRGB());
		x11.XMapWindow(display, borderWindow);
		// commit changes and close display
		x11.XFlush(display);
		x11.XCloseDisplay(display);
		return borderWindow;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.cube.client.wm.x.XXWindowManager#removeWindow(ch.admin.vbs
	 * .cube.client.wm.x.X11.Window)
	 */
	public void removeWindow(Window window) {
		Display display = x11.XOpenDisplay(displayName);
		x11.XDestroyWindow(display, window);
		// commit changes and close display
		x11.XFlush(display);
		x11.XCloseDisplay(display);
	}

	/**
	 * Reparents the inside window to the new parent window.
	 * 
	 * @param parentWindow
	 *            the new parent of the inside window
	 * @param childWindow
	 *            the window which will be child of the new parent window
	 * @param x
	 *            the x position of the child window
	 * @param y
	 *            the y positon of the child window
	 */
	private void reparentWindow(Window parentWindow, Window childWindow, int x, int y) {
		Display display = x11.XOpenDisplay(displayName);
		// Map the virtual machine window as child to the border window
		x11.XReparentWindow(display, childWindow, parentWindow, x, y);
		// commit changes and close display
		x11.XFlush(display);
		x11.XCloseDisplay(display);
		LOG.debug(String.format("reparentWindow() - child[%s / %s]  parent[%s / %s] [%d x %d]\n", getWindowName(childWindow), childWindow,
				getWindowName(parentWindow), parentWindow, x, y));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.cube.client.wm.x.XXWindowManager#reparentWindowAndResize
	 * (ch.admin.vbs.cube.client.wm.x.X11.Window,
	 * ch.admin.vbs.cube.client.wm.x.X11.Window, java.awt.Rectangle)
	 */
	public void reparentWindowAndResize(Window parentWindow, Window childWindow, Rectangle bounds) {
		Display display = x11.XOpenDisplay(displayName);
		// move border window to new CubeFrame
		x11.XReparentWindow(display, childWindow, parentWindow, bounds.x, bounds.y);
		// resize border window to the new CubeFrame size
		x11.XMoveResizeWindow(display, childWindow, bounds.x, bounds.y, bounds.width, bounds.height);
		x11.XFlush(display);
		// close display
		x11.XCloseDisplay(display);
		// -----------------
		display = x11.XOpenDisplay(displayName);
		long[] childrenWindowIdArray = getChildrenList(display, childWindow);
		for (long windowId : childrenWindowIdArray) {
			Window window = new Window(windowId);
			LOG.debug("---> reparentWindowAndResize() send ConfigureNotify to [{}] name[{}]", window, getWindowName(window));
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
			x11.XSendEvent(display, window, 1, event_mask, event);
		}
		x11.XCloseDisplay(display);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.admin.vbs.cube.client.wm.x.XXWindowManager#destroy()
	 */
	public synchronized void destroy() {
		if (!destroyed) {
			destroyed = true;
			if (eventThread != null) {
				eventThread.interrupt();
				eventThread = null;
			}
		}
	}

	/**
	 * Registers the window for all interesting events.
	 * 
	 * @param window
	 *            the window which events will be catched
	 */
	private void registerWindowForEvents(Window window) {
		// select the events that this window will get
		// x11.XSelectInput(eventDisplay, window, new
		// NativeLong(X11.ResizeRedirectMask | X11.EnterWindowMask ));
		x11.XSelectInput(eventDisplay, window, new NativeLong(X11.ResizeRedirectMask | X11.EnterWindowMask | X11.ConfigureNotify));
		x11.XFlush(eventDisplay);
	}

	private void registerWindowForExtraEvents(Window window) {
		// register for events
		x11.XSelectInput(eventDisplay, window, new NativeLong(X11.PropertyChangeMask));
		x11.XFlush(eventDisplay);
	}

	public void registerRootWindowEvents() {
		Window root = findRootWindow();
		// register for events
		x11.XSelectInput(eventDisplay, root, new NativeLong(X11.SubstructureNotifyMask | X11.PropertyChangeMask));
		x11.XFlush(eventDisplay);
	}

	/**
	 * Processes events which has been registered.
	 * 
	 * @param event
	 *            the thrown event
	 * @see XWindowManager#registerWindowForEvents(Window)
	 */
	private void processEvent(XEvent event) {
		if (!destroyed) {
			switch (event.type) {
			case X11.ResizeRequest:
				// Loads the correct type for the event and fills the
				// attributes, just event.xresizerequest does not
				// work!
				X11.XResizeRequestEvent resizeRequest = (X11.XResizeRequestEvent) event.getTypedValue(X11.XResizeRequestEvent.class);
				if (LOG.isDebugEnabled()) {
					LOG.debug("ResizeRequest for window " + resizeRequest.window.longValue());
				}
				reactOnResizeRequest(resizeRequest.window, resizeRequest.width, resizeRequest.height);
				break;
			case X11.EnterNotify:
				X11.XCrossingEvent enterWindowEvent = (X11.XCrossingEvent) event.getTypedValue(X11.XCrossingEvent.class);
				// set focus
				x11.XSetInputFocus(eventDisplay, enterWindowEvent.window, X11.RevertToParent, new NativeLong(0));
			case X11.PropertyNotify: {
				X11.XPropertyEvent xe = (X11.XPropertyEvent) event.getTypedValue(X11.XPropertyEvent.class);
				String atomName = x11.XGetAtomName(eventDisplay, xe.atom);
				if ("WM_NAME".equals(atomName)) {
					notifyWindowUpdated(xe.window);
				}
			}
				break;
			case X11.CreateNotify: {
				X11.XCreateWindowEvent xe = (X11.XCreateWindowEvent) event.getTypedValue(X11.XCreateWindowEvent.class);
				registerWindowForExtraEvents(xe.window);
				notifyWindowCreated(xe.window);
			}
				break;
			case X11.ClientMessage: {
				// noisy !!
				// X11.XClientMessageEvent xe = (X11.XClientMessageEvent)
				// event.getTypedValue(X11.XClientMessageEvent.class);
				// String atomName = x11.XGetAtomName(eventDisplay,
				// xe.message_type);
				// LOG.debug("X11.ClientMessage [" + atomName + "] changed");
			}
				break;
			case X11.DestroyNotify: {
				X11.XDestroyWindowEvent xe = (X11.XDestroyWindowEvent) event.getTypedValue(X11.XDestroyWindowEvent.class);
				notifyWindowDestroyed(xe.window);
			}
				break;
			case X11.MapNotify: {
				X11.XMapEvent xe = (X11.XMapEvent) event.getTypedValue(X11.XMapEvent.class);
				XTextProperty windowTitle = new XTextProperty();
				x11.XFetchName(eventDisplay, xe.window, windowTitle);
			}
				break;
			case X11.ConfigureNotify:
			case X11.UnmapNotify:
			default:
				// noisy !!
				// LOG.error("Ignore XEvent [{}]", event.type);
				break;
			}
		}
	}

	private void notifyWindowUpdated(Window window) {
		cb.windowUpdated(window);
	}

	private void notifyWindowCreated(Window window) {
		cb.windowCreated(window);
	}

	private void notifyWindowDestroyed(Window window) {
		cb.windowDestroyed(window);
	}

	@Override
	public String getWindowName(Window w) {
		XTextProperty windowTitle = new XTextProperty();
		x11.XFetchName(eventDisplay, w, windowTitle);
		return windowTitle.value;
	}

	/**
	 * Checks if the resized event resizes the window to the propriety size,
	 * otherwise it resize it new with the inside width of his parent window.
	 * 
	 * @param window
	 *            the window which was requested to be resized
	 * @param width
	 *            the width of the window, to be requested to be resized
	 * @param height
	 *            the heihgt of the window, to be requested to be resized
	 */
	private synchronized void reactOnResizeRequest(Window window, int width, int height) {
		// prepare reference values
		WindowByReference rootWindowRef = new WindowByReference();
		WindowByReference parentWindowRef = new WindowByReference();
		PointerByReference childrenPtr = new PointerByReference();
		IntByReference childrenCount = new IntByReference();
		// find the parent to the window
		if (x11.XQueryTree(eventDisplay, window, rootWindowRef, parentWindowRef, childrenPtr, childrenCount) == 0) {
			if (LOG.isErrorEnabled()) {
				LOG.error("BadWindow - A value for a Window argument does not name a defined Window!");
			}
			return;
		}
		// get parent attributes (width and height)
		XWindowAttributes parentAttributes = new XWindowAttributes();
		x11.XGetWindowAttributes(eventDisplay, parentWindowRef.getValue(), parentAttributes);
		int insideWidth = parentAttributes.width;
		int insideHeight = parentAttributes.height;
		// check if resize needs to be changed
		if (width != insideWidth || height != insideHeight) {
			x11.XMoveResizeWindow(eventDisplay, window, 0, 0, insideWidth, insideHeight);
			x11.XFlush(eventDisplay);
		} else {
			LOG.debug("Ignore resize request.");
			x11.XMoveResizeWindow(eventDisplay, window, 0, 0, insideWidth, insideHeight);
			x11.XFlush(eventDisplay);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.admin.vbs.cube.client.wm.x.XXWindowManager#adjustScreenForChild(ch
	 * .admin.vbs.cube.client.wm.x.X11.Window, int, int)
	 */
	public void adjustScreenForChild(Window parentWindow, int width, int height) {
		Display display = x11.XOpenDisplay(displayName);
		// find the running vm and if there is one resize it
		long[] childrenWindowIdArray = getChildrenList(display, parentWindow);
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
				x11.XResizeWindow(display, window, width, height);
			}
		}
		// commit changes and close display
		x11.XFlush(display);
		x11.XCloseDisplay(display);
	}

	@Override
	public void setWindowManagerCallBack(IWindowManagerCallback cb) {
		this.cb = cb;
	}

//	public void list() {//DEBUG DEBUG DEBUIG
//		Window foundWindow = null;
//		Display display = x11.XOpenDisplay(displayName);
//		// get the root window
//		Window rootWindow = x11.XRootWindow(display, screenIndex);
//		long[] childrenWindowIdArray = getChildrenList(display, rootWindow);
//		for (long windowId : childrenWindowIdArray) {
//			Window window = new Window(windowId);
//			// get window attributes
//			XWindowAttributes attributes = new XWindowAttributes();
//			x11.XGetWindowAttributes(display, window, attributes);
//			// get window title
//			XTextProperty windowTitle = new XTextProperty();
//			x11.XFetchName(display, window, windowTitle);
//			// filter windows with attributes which our windows do not have
//			if (!attributes.override_redirect && windowTitle.value != null) {
//				LOG.warn("Scan windows [{}]",windowTitle.value);
//		
//			}
//		}
//		// close display
//		x11.XCloseDisplay(display);
//	}
}
