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

import java.awt.Color;
import java.awt.Rectangle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.IMonitorLayout;
import ch.admin.vbs.cube.atestwm.IScreenManager;
import ch.admin.vbs.cube.atestwm.ITabManager;
import ch.admin.vbs.cube.atestwm.IWindowManager;
import ch.admin.vbs.cube.atestwm.IXrandrMonitor;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Display;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.WindowByReference;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XEvent;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XTextProperty;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XWindowChanges;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class XSimpleWindowManager implements IWindowManager {
	private enum Arch {
		OS_32_BIT, OS_64_BIT
	}

	private static final Logger LOG = LoggerFactory.getLogger(XSimpleWindowManager.class);
	private Arch osArch;
	protected static final long MAX_LOCK_TIMEOUT = 5000;
	private X11 x11;
	private String displayName;
	private int screenIndex;
	private Display display;
	private Thread eventThread;
	// lock monitoring stuff (debug purpose)
	private Lock lock = new ReentrantLock();
	private long lockTimestamp = 0;
	private String lockCommand;
	protected boolean destroyed;
	//
	// private SimpleManagedWindowModel managedWindowModel = new
	// SimpleManagedWindowModel();
	private IXrandrMonitor monMgr;
	private IXrandr xrandr;
	private ITabManager tabManager;
	private IScreenManager screenManager;

	public XSimpleWindowManager() {
		// Find out default display name
		setDisplayName(System.getenv("DISPLAY"));
	}

	/** set display name */
	public void setDisplayName(String name) {
		if (name == null) {
			name = ":0.0";
			screenIndex = 0;
		} else {
			// displayName is something like this ":<display>.<screen>"
			int dotIndex = name.indexOf(".");
			if (dotIndex < 0) {
				// no screen specified
				screenIndex = 0;
				displayName = name;
			} else {
				// parse screen index
				screenIndex = Integer.parseInt(name.substring(dotIndex + 1));
				displayName = name.substring(0, dotIndex);
			}
		}
	}

	public void start() {
		x11 = X11.INSTANCE;
		x11.XInitThreads();
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
		// open display
		LOG.debug("Start WindowManager..");
		display = x11.XOpenDisplay(displayName);
		// Trap root window's events (see blackbox:Screen.cc)
		x11.XSelectInput(display, x11.XRootWindow(display, screenIndex), new NativeLong(//
				X11.PropertyChangeMask //
						| X11.StructureNotifyMask //
						| X11.SubstructureRedirectMask //
						| X11.ButtonPressMask));
		x11.XSync(display, false);
		// start event processing thread
		eventThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					XEvent event = new XEvent();
					while (!destroyed) {
						do {
							while (!destroyed && x11.XEventsQueued(display, X11.QueuedAlready) > 0) {
								try {
									x11.XNextEvent(display, event);
									// process event
									processEvent(event);
								} catch (Exception e) {
									LOG.error("Error during catching events", e);
								}
							}
						} while (!destroyed && x11.XEventsQueued(display, X11.QueuedAfterFlush) > 0);
					}
					// close display once thread stop (only when cube shutdown)
					x11.XCloseDisplay(display);
				} catch (Exception e) {
					LOG.error("XEvent Thread Failed");
				}
			}
		}, "XEvent Processor for Cube");
		eventThread.start();
	}

	private void processEvent(XEvent event) {
		if (!destroyed) {
			switch (event.type) {
			case X11.MapRequest:
				handleMapRequest(event);
				break;
//			case X11.ConfigureRequest:
//				handleConfigureRequest(event);
//				break;
			// case X11.DestroyNotify:
			// handleDestroyNotify(event);
			// break;
			// case X11.ConfigureNotify:
			// handleConfigureNotify(event);
			// break;
//			case X11.ResizeRequest:
//				handleResizeRequest(event);
//				break;
			default:
				LOG.debug("Got an XEvent [{}]", X11.XEventName.getEventName(event.type));
				break;
			}
		}
	}

	// private void handleDestroyNotify(XEvent event) {
	// lock();
	// try {
	// X11.XConfigureRequestEvent e = (X11.XConfigureRequestEvent)
	// event.getTypedValue(X11.XConfigureRequestEvent.class);
	// LOG.debug(String.format("XConfigureRequestEvent for window [%s]",
	// getWindowNameNoLock(e.window)));
	// // // check if managed
	// // ManagedWindow managed =
	// // managedWindowModel.getManagedByClient(e.window);
	// // if (managed != null) {
	// // x11.XUnmapWindow(display, managed.getBorder());
	// // x11.XDestroyWindow(display, managed.getBorder());
	// // x11.XFlush(display);
	// // managedWindowModel.remove(managed);
	// // }
	// } finally {
	// unlock();
	// }
	// }
	private void handleResizeRequest(XEvent event) {
		lock();
		try {
			X11.XResizeRequestEvent e = (X11.XResizeRequestEvent) event.getTypedValue(X11.XResizeRequestEvent.class);
			String winName = getWindowNameNoLock(e.window);
			LOG.debug(String.format("XResizeRequestEvent for [%s] (%dx%d)", winName, e.width, e.height));
			Window tabWindow = screenManager.getTabWindow(winName);
			if (tabWindow != null) {
				Rectangle tabBnd = screenManager.getTabWindowBounds(winName);
				LOG.debug(String.format("IT IS A TAB PANEL [%s] (%dx%d)", winName, e.width, e.height));
//				if (tabBnd.x != 0 || tabBnd.x != 0 || tabBnd.y != e.y || tabBnd.width != e.width) {
//					// x11.XMoveResizeWindow(display, e.window, 0, 0,
//					// tabBnd.width, tabBnd.height);
//					// LOG.debug("Wrong resize/move : force it");
//				}
				x11.XResizeWindow(display, e.window, e.width, e.height);
			}
		} finally {
			unlock();
		}
	}

	private void handleConfigureRequest(XEvent event) {
		lock();
		try {
			X11.XConfigureRequestEvent e = (X11.XConfigureRequestEvent) event.getTypedValue(X11.XConfigureRequestEvent.class);
			String winName = getWindowNameNoLock(e.window);
			LOG.debug(String.format("XConfigureRequestEvent for [%s] (%d:%d)(%dx%d)", winName, e.x, e.y, e.width, e.height));
			Window tabWindow = screenManager.getTabWindow(winName);
			if (tabWindow != null) {
				Rectangle tabBnd = screenManager.getTabWindowBounds(winName);
				// A tab frame try to configure its size/position
				LOG.debug(String.format("IT IS A TAB PANEL [%s] (%d:%d)(%dx%d)", winName, e.x, e.x, e.width, e.height));
				if ((e.value_mask.longValue() & X11.PSize) != 0 && (e.value_mask.longValue() & X11.PPosition) != 0) {
					if (tabBnd.x != 0 || tabBnd.x != 0 || tabBnd.y != e.y || tabBnd.width != e.width) {
						LOG.debug("Wrong application resize/move : fix it");
						x11.XConfigureWindow(display, e.window, e.value_mask, prepareChgX(0, 0, e.width, e.height, e.border_width, e.detail, e.above));
					}
				}
//				if ((e.value_mask.longValue() & X11.USSize) != 0 && (e.value_mask.longValue() & X11.USPosition) != 0) {
//					if (tabBnd.x != 0 || tabBnd.x != 0 || tabBnd.y != e.y || tabBnd.width != e.width) {
//						LOG.debug("Wrong user resize/move : fix it");
//						x11.XConfigureWindow(display, e.window, e.value_mask, prepareChgX(0, 0, e.width, e.height, e.border_width, e.detail, e.above));
//					}
//				}
				// if ((e.value_mask.longValue() & X11.USSize) != 0 &&
				// (e.value_mask.longValue() & X11.USPosition) != 0) {
				// if (tabBnd.x != 0 || tabBnd.x != 0 || tabBnd.y != e.y ||
				// tabBnd.width != e.width) {
				// x11.XMoveResizeWindow(display, e.window, 0, 0, tabBnd.width,
				// tabBnd.height);
				// LOG.debug("Wrong user resize/move : force it");
				// }
				// }
			} else {
				LOG.debug("Othe window configure request [{}]", winName);
			}
			//
			// // check if managed
			// ManagedWindow managed =
			// managedWindowModel.getManagedByClient(e.window);
			// if (managed == null) {
			// // TODO: there is race condition there risk there. see
			// // blackbox.cc:154
			// // configure window as it want it
			// LOG.debug(String.format("XConfigureRequestEvent for unmanaged window [%s] (%d:%d)(%dx%d)",
			// getWindowNameNoLock(e.window), e.x, e.y, e.width, e.height));
			// x11.XConfigureWindow(display, e.window, e.value_mask,
			// prepareChgX(e.x, e.y, e.width, e.height, e.border_width,
			// e.detail, e.above));
			// } else {
			// switch (managed.getType()) {
			// case TABS:
			// LOG.debug("XConfigureRequestEvent for TAB -> TODO");
			// layout.??
			// break;
			// default:
			// LOG.debug("XConfigureRequestEvent for managed window (parent) [%s]",getWindowNameNoLock(e.parent));
			// x11.XConfigureWindow(display, e.parent, e.value_mask,
			// prepareChgX(e.x, e.y, e.width, e.height, e.border_width,
			// e.detail, e.above));
			// LOG.debug("XConfigureRequestEvent for managed window (client) [%s]",getWindowNameNoLock(e.parent));
			// x11.XConfigureWindow(display, e.window, e.value_mask,
			// prepareChgX(0,0, e.width, e.height, e.border_width, e.detail,
			// e.above));
			// break;
			// }
			// }
		} finally {
			unlock();
		}
	}

	private XWindowChanges prepareChgX(int x, int y, int w, int h, int border, int details, Window sibling) {
		XWindowChanges chg = new XWindowChanges();
		chg.height = h;
		chg.width = w;
		chg.border_width = border;
		chg.sibling = sibling;
		chg.x = x;
		chg.y = y;
		chg.stack_mode = details;
		LOG.debug(String.format("XWindowChanges (%d:%d)(%dx%d)", chg.x, chg.y, chg.width, chg.height));
		return chg;
	}

	private void handleMapRequest(XEvent event) {
		lock();
		try {
			X11.XMapEvent e = (X11.XMapEvent) event.getTypedValue(X11.XMapEvent.class);
			String winName = getWindowNameNoLock(e.window);
			if (tabManager.isTabPanel(winName)) {
				// it is a tab panel -> authorize mapping. re-parent to tab
				// window
				Window tapWin = screenManager.getTabWindow(winName);
				if (tapWin == null) {
					// should not happend
					LOG.error("TabFrame want Map but the Window is not ready yet");
					return;
				}
				LOG.warn("Process MapRequest for window [{}]", winName);
				x11.XReparentWindow(display, e.window, tapWin, 0, 0);
				x11.XChangeSaveSet(display, e.window, X11.SetModeInsert);
				x11.XSelectInput(display, e.window, new NativeLong(X11.PropertyChangeMask | X11.SubstructureNotifyMask | X11.ResizeRedirectMask
						| X11.StructureNotifyMask));
				x11.XFlush(display);
				// effectively map client
				x11.XMapWindow(display, e.window);
				x11.XFlush(display);
			} else {
				LOG.warn("Ignore MapRequest for window [{}]", winName);
			}
		} finally {
			unlock();
		}
	}

	public Window createAndMapWindow(Rectangle bounds) {
		Window w = createBorderWindow(x11.XRootWindow(display, screenIndex), 1, Color.RED, Color.ORANGE, bounds);
		x11.XMapRaised(display, w);
		x11.XFlush(display);
		return w;
	}

	@Override
	public void moveAndResizeWindow(Window window, Rectangle bnd) {
		LOG.debug("move window [{}]", BoundFormatterUtil.format(bnd));
		x11.XMoveResizeWindow(display, window, bnd.x, bnd.y, bnd.width, bnd.height);
		x11.XFlush(display);
	}

	@Override
	public void disposeWindow(Window window) {
		x11.XUnmapWindow(display, window);
		x11.XDestroyWindow(display, window);
		x11.XFlush(display);
	}

	private Window createBorderWindow(Window frame, int borderSize, Color borderColor, Color backgroundColor, Rectangle bounds) {
		lock();
		try {
			/*
			 * connection to the x server and set resources permanent, otherwise
			 * window would be destroyed by calling XCloseDisplay
			 */
			x11.XSetCloseDownMode(display, X11.RetainPermanent);
			/*
			 * Create window which will hold the virtual machine window and
			 * paints a border. This border window is a child of the parent
			 * window.
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
			LOG.debug(String.format("Bordered window created [id:%s] %s", borderWindow, BoundFormatterUtil.format(bounds)));
			// flush
			x11.XFlush(display);
			return borderWindow;
		} finally {
			unlock();
		}
	}

	private String getWindowNameNoLock(Window w) {
		if (w == null)
			return null;
		XTextProperty windowTitle = new XTextProperty();
		x11.XFetchName(display, w, windowTitle);
		return windowTitle.value;
	}

	private void lock() {
		lock.lock();
		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		lockCommand = ste[ste.length - 1].getMethodName();
		lockTimestamp = System.currentTimeMillis();
	}

	private void unlock() {
		final long delta = System.currentTimeMillis() - lockTimestamp;
		if (delta > MAX_LOCK_TIMEOUT) {
			/**
			 * Method call MUST be fast in order to guarantee a good user
			 * experience (avoid freezing UI)
			 */
			LOG.error("Method [" + lockCommand + "] call duration timeout [" + delta + " ms] [" + lockTimestamp + "].");
		}
		lockTimestamp = 0;
		lock.unlock();
	}

	public void setup(IXrandrMonitor monMgr, IXrandr xrandr, IMonitorLayout layout, ITabManager tabManager, IScreenManager screenManager) {
		this.monMgr = monMgr;
		this.xrandr = xrandr;
		this.tabManager = tabManager;
		this.screenManager = screenManager;
	}

	// ####################
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
}
