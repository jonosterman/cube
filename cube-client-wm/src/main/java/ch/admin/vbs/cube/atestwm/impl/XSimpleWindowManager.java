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

import ch.admin.vbs.cube.atestwm.IMessageManager;
import ch.admin.vbs.cube.atestwm.IMonitorLayout;
import ch.admin.vbs.cube.atestwm.IScreenManager;
import ch.admin.vbs.cube.atestwm.ITabManager;
import ch.admin.vbs.cube.atestwm.IWindowManager;
import ch.admin.vbs.cube.atestwm.IXrandrMonitor;
import ch.admin.vbs.cube.atestwm.impl.ScreenManager.Screen;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Display;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.WindowByReference;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XConfigureEvent;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XEvent;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XSizeHints;
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
	private IXrandrMonitor monMgr;
	private IXrandr xrandr;
	private ITabManager tabManager;
	private IMessageManager msgManager;
	private IScreenManager screenManager;
	//
	private MWindowModel wmodel = new MWindowModel();

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
		// Trap root window's events (see metacity Screen.c:450)
		x11.XSelectInput(display, x11.XRootWindow(display, screenIndex), new NativeLong(//
				X11.SubstructureRedirectMask //
						| X11.SubstructureNotifyMask //
						| X11.PropertyChangeMask //
						| X11.EnterWindowMask //
						| X11.LeaveWindowMask //
						| X11.ColormapChangeMask //
						| X11.KeyPressMask //
						| X11.KeyReleaseMask //
						| X11.FocusChangeMask //
						| X11.StructureNotifyMask //
		));
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
			case X11.ConfigureRequest:
				handleConfigureRequest(event);
				break;
			// case X11.DestroyNotify:
			// handleDestroyNotify(event);
			// break;
			case X11.ConfigureNotify:
				handleConfigureNotify(event);
				break;
			case X11.ResizeRequest:
				handleResizeRequest(event);
				break;
			case X11.EnterNotify: {
				lock();
				try {
					// we catch EnterNotify to ensure that the visible VM will
					// get
					// the keyboard/mouse focus.
					X11.XCrossingEvent enterWindowEvent = (X11.XCrossingEvent) event.getTypedValue(X11.XCrossingEvent.class);
					x11.XSetInputFocus(display, enterWindowEvent.window, X11.RevertToParent, new NativeLong(0));
				} finally {
					unlock();
				}
			}
				break;
			default:
				LOG.debug("({}) Ignore.", X11.XEventName.getEventName(event.type));
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
			MWindow mw = wmodel.getMWindowByClient(e.window);
			if (mw != null) {
				// check if size match our own constraints
				Rectangle xBnd = mw.getXBounds();
				if (xBnd.width != e.width || xBnd.height != e.height) {
					LOG.debug(String.format("(XResizeRequestEvent) [%s] (managed) (%dx%d) when should (%dx%d) : Missmatch. Enforce.", //
							winName, e.width, e.height, xBnd.width, xBnd.height));
					x11.XResizeWindow(display, e.window, xBnd.width, xBnd.height);
					sendResizeEvent(display, mw);
				} else {
					LOG.debug(String.format("(XResizeRequestEvent) [%s] (managed) (%dx%d) :  Ignore.", //
							winName, e.width, e.height));
				}
			} else {
				LOG.debug("(XResizeRequestEvent) [{}] (unmanaged). Ignore.", winName);
			}
		} finally {
			unlock();
		}
	}

	private void handleConfigureNotify(XEvent event) {
		lock();
		try {
			X11.XConfigureEvent e = (X11.XConfigureEvent) event.getTypedValue(X11.XConfigureEvent.class);
			String winName = getWindowNameNoLock(e.window);
			if (e.window == null) {
				LOG.debug("(ConfigureNotify) [null]. Ignore.");
				return;
			}
			MWindow mw = wmodel.getMWindowByClient(e.window);
			if (mw != null) {
				// check if size match our own constraints
				Rectangle xBnd = mw.getXBounds();
				Rectangle eBnd = mw.getExternBounds();
				if (xBnd.width != e.width || xBnd.height != e.height || eBnd.x != e.x || eBnd.y != e.y) {
					LOG.debug(String.format(
							"(ConfigureNotify) [%s] (managed) (%d:%d)(%dx%d) when should (%d:%d)(%dx%d) : Missmatch. Force move resize and send event.", //
							winName, e.x, e.y, e.width, e.height, eBnd.x, eBnd.y, xBnd.width, xBnd.height));
					// move client to (0,0) (since it it a children window of
					// the managed window)
					x11.XMoveResizeWindow(display, e.window, 0, 0, xBnd.width, xBnd.height);
					// send configure event (with the right coordinates so it
					// knows where to place pop-ups, etc.)
					sendResizeEvent(display, mw);
					x11.XFlush(display);
				} else {
					LOG.debug(String.format("(ConfigureNotify) [%s] (managed) (%d:%d)(%dx%d) : Correct. Ignore.", //
							winName, e.x, e.y, e.width, e.height));
				}
			} else {
				LOG.debug("(ConfigureNotify) [{}] (unmanaged). Ignore.", winName);
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
			MWindow mw = wmodel.getMWindowByClient(e.window);
			if (mw == null) {
				LOG.debug(String.format(
						"(XConfigureRequestEvent) [%s] (unmanaged) (%d:%d)(%dx%d) [ppos:%b, psize:%b, upos:%b, usize:%b]. Apply as-is.", //
						winName, e.x, e.y, e.width,
						e.height, //
						(e.value_mask.longValue() & X11.PPosition) > 0, (e.value_mask.longValue() & X11.PSize) > 0,
						(e.value_mask.longValue() & X11.USPosition) > 0, (e.value_mask.longValue() & X11.USSize) > 0));
				x11.XConfigureWindow(display, e.window, e.value_mask, prepareChgX(e.x, e.y, e.width, e.height, e.border_width, e.send_event, e.above));
			} else {
				// see void BlackboxWindow::configure for details (this is a
				// simplified/hacked version of it.)
				boolean posOrSizeReq = (e.value_mask.longValue() & (X11.CWX | X11.CWX | X11.CWWidth | X11.CWHeight)) > 0;
				// boolean sendEvent = (e.x != bnd.x) || (e.y != bnd.y); // see
				// ICCCM specs
				if (posOrSizeReq) {
					LOG.debug(String.format("(XConfigureRequestEvent) [%s] (managed)", winName));
					sendResizeEvent(display, mw);
				} else {
					LOG.debug(String.format("(XConfigureRequestEvent) [%s] (managed) without x, y, width or height: Ignore.", winName));
				}
			}
			x11.XFlush(display);
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
		return chg;
	}

	private void handleMapRequest(XEvent event) {
		lock();
		try {
			X11.XMapEvent e = (X11.XMapEvent) event.getTypedValue(X11.XMapEvent.class);
			String winName = getWindowNameNoLock(e.window);
			if (tabManager.matchTabPanel(winName) || msgManager.matchMsgPanel(winName)) {
				// it is a tab panel -> authorize mapping. re-parent to tab
				// window
				MWindow mw = screenManager.getTabOrMsgWindow(winName);
				if (mw == null) {
					// should never happen
					LOG.error("TabFrame want Map but the Window is not ready yet");
					return;
				}
				LOG.debug(String.format("(XMapRequest) [%s] (tabs frame). Reparent and map.", winName));
				// re-parent
				mw.setXclient(e.window);
				x11.XReparentWindow(display, e.window, mw.getXWindow(), 0, 0);
				x11.XChangeSaveSet(display, e.window, X11.SetModeInsert);
				x11.XSelectInput(display, e.window, //
						new NativeLong(X11.PropertyChangeMask //
								| X11.SubstructureNotifyMask //
								// | X11.SubstructureRedirectMask // debug
								| X11.ResizeRedirectMask //
								| X11.StructureNotifyMask));
				x11.XFlush(display);
				// Map client and managed windows
				x11.XMapWindow(display, mw.getXClient());
				x11.XMapSubwindows(display, mw.getXWindow());
				x11.XMapWindow(display, mw.getXWindow());
				x11.XFlush(display);
			} else if (winName != null && winName.endsWith(" - Oracle VM VirtualBox")) {
				// it is a VM -> authorize mapping. re-parent to border window
				MWindow mw = screenManager.getAppWindow(e.window);
				if (mw == null) {
					// app window does not exists (unexpected but possible)
					Screen defScr = screenManager.getDefaultScreen();
					// create managed window
					Rectangle parentXBnd = defScr.bgWindow.getXBounds();
					// x & y at parent origin. width & height smaller due to child borders (see X spec.) 
					Rectangle childBnd = new Rectangle(parentXBnd.x, parentXBnd.y, parentXBnd.width-2*MWindow.BORDER_WM, parentXBnd.height-2*MWindow.BORDER_WM);
					Window w = createBorderWindow(x11.XRootWindow(display, screenIndex), 2, Color.GREEN, Color.BLACK, childBnd);
					x11.XSelectInput(display, w, new NativeLong(X11.NoEventMask));
					mw = new MWindow(w, parentXBnd, 2);
					x11.XMapRaised(display, w);
					x11.XFlush(display);
					// add to list of managed windows
					wmodel.addWindow(mw);
					// add to screen's appWindows
					defScr.appWindows.add(mw);
				}
				LOG.debug(String.format("(XMapRequest) [%s] (VM). Reparent and map.", winName));
				// re-parent
				mw.setXclient(e.window);
				x11.XReparentWindow(display, e.window, mw.getXWindow(), 0, 0);
				x11.XChangeSaveSet(display, e.window, X11.SetModeInsert);
				x11.XSelectInput(display, e.window, //
						new NativeLong(X11.PropertyChangeMask //
								| X11.SubstructureNotifyMask //
								| X11.ResizeRedirectMask //
								| X11.StructureNotifyMask));
				x11.XFlush(display);
				// Map client and managed windows
				x11.XMapWindow(display, mw.getXClient());
				x11.XMapSubwindows(display, mw.getXWindow());
				x11.XMapWindow(display, mw.getXWindow());
				x11.XFlush(display);
			} else {
				LOG.warn("(XMapRequest) [{}]. Ignore.", winName);
			}
		} finally {
			unlock();
		}
	}

	// ==========================================
	public MWindow createAndMapWindow(Rectangle externBnds, int border) {
		Rectangle xBnd = new Rectangle(externBnds.x, externBnds.y, externBnds.width - 2 * border, externBnds.height - 2 * border);
		// TODO: black and no border
		Window w = createBorderWindow(x11.XRootWindow(display, screenIndex), border, Color.RED, Color.ORANGE, xBnd);
		x11.XSelectInput(display, w, new NativeLong(X11.NoEventMask));
		MWindow mw = new MWindow(w, externBnds, MWindow.BORDER_DEB);
		x11.XMapRaised(display, w);
		x11.XFlush(display);
		wmodel.addWindow(mw);
		return mw;
	}

	@Override
	public void moveAndResizeWindow(MWindow mw, Rectangle newBnds) {
		LOG.debug("move window [{}]", BoundFormatterUtil.format(newBnds));
		mw.setBounds(newBnds);
		x11.XMoveResizeWindow(display, mw.getXWindow(), newBnds.x, newBnds.y, newBnds.width, newBnds.height);
		// sendResizeEvent(display, mw.getXClient(), mw.getBounds(),
		// mw.getXWindow());
		XConfigureEvent cevent = new XConfigureEvent();
		cevent.window = mw.getXClient();
		cevent.event = mw.getXClient();
		cevent.type = X11.ConfigureNotify;
		cevent.display = display;
		// need proper x,y or popup will show up elsewhere
		cevent.x = newBnds.x;
		cevent.y = newBnds.y;
		cevent.height = newBnds.height;
		cevent.width = newBnds.width;
		cevent.border_width = 0;
		cevent.above = mw.getXWindow();
		cevent.override_redirect = 0;
		x11.XSendEvent(display, mw.getXClient(), 0, new NativeLong(X11.StructureNotifyMask), cevent);
		x11.XFlush(display);
	}

	@Override
	public void disposeWindow(MWindow mw) {
		x11.XUnmapWindow(display, mw.getXWindow());
		x11.XDestroyWindow(display, mw.getXWindow());
		wmodel.removeWindow(mw);
		x11.XFlush(display);
	}

	// ==========================================
	private Window createBorderWindow(Window parent, int borderSize, Color borderColor, Color backgroundColor, Rectangle bounds) {
		lock();
		try {
			/*
			 * connection to the x server and set resources permanent, otherwise
			 * window would be destroyed by calling XCloseDisplay
			 */
			// not wanted for WM windows. x11.XSetCloseDownMode(display,
			// X11.RetainPermanent);
			/*
			 * Create window which will hold the virtual machine window and
			 * paints a border. This border window is a child of the parent
			 * window.
			 */
			Window borderWindow = x11.XCreateSimpleWindow(//
					display, //
					parent, //
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

	public void setup(IXrandrMonitor monMgr, IXrandr xrandr, IMonitorLayout layout, ITabManager tabManager, IScreenManager screenManager, IMessageManager msgManager) {
		this.monMgr = monMgr;
		this.xrandr = xrandr;
		this.tabManager = tabManager;
		this.screenManager = screenManager;
		this.msgManager = msgManager;
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

	private void sendResizeEvent(Display display, MWindow mw) {
		Rectangle xBnd = mw.getXBounds();
		Rectangle eBnd = mw.getExternBounds();
		LOG.debug("-> sendResizeEvent() [extBnd: {}] " + BoundFormatterUtil.format(eBnd), getWindowNameNoLock(mw.getXClient()));
		//
		XSizeHints hints = x11.XAllocSizeHints();
		hints.base_width = xBnd.height;
		hints.base_height = xBnd.width;
		hints.x = eBnd.x;
		hints.y = eBnd.y;
		hints.flags = new NativeLong(X11.PBaseSize);
		x11.XSetWMNormalHints(display, mw.getXClient(), hints);
		// Send an event to force application (VirtualBox) to resize. It is
		// needed if we move the VM on a 2nd screen with another resolution
		// than the first one.
		XConfigureEvent event = new XConfigureEvent();
		event.window = mw.getXClient();
		event.event = mw.getXClient();
		event.type = X11.ConfigureNotify;
		event.display = display;
		event.x = eBnd.x;
		event.y = eBnd.y;
		event.height = xBnd.height;
		event.width = xBnd.width;
		event.border_width = 0;
		event.above = mw.getXWindow();
		event.override_redirect = 0;
		x11.XSendEvent(display, mw.getXClient(), 1, new NativeLong(X11.StructureNotifyMask), event);
		// flush
		x11.XFlush(display);
	}
}
