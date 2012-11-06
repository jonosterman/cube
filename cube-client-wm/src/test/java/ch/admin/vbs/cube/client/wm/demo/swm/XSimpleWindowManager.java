package ch.admin.vbs.cube.client.wm.demo.swm;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Display;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XEvent;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XTextProperty;

import com.sun.jna.NativeLong;

public class XSimpleWindowManager {
	private static final Logger LOG = LoggerFactory.getLogger(XSimpleWindowManager.class);
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
	SimpleManagedWindowModel managedWindowModel = new SimpleManagedWindowModel();

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
		// open display
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
		LOG.debug("Got an XEvent [{}]", X11.XEventName.getEventName(event.type));
		if (!destroyed) {
			switch (event.type) {
			case X11.MapRequest:
				handleMapRequest(event);
			}
		}
	}

	private void handleMapRequest(XEvent event) {
		lock();
		try {
			X11.XMapEvent e = (X11.XMapEvent) event.getTypedValue(X11.XMapEvent.class);
			if (managedWindowModel.isManaged(e.window)) {
				LOG.debug(String.format("XMapEvent for managed window [%s]", getWindowNameNoLock(e.window)));
				// ??
			} else {
				LOG.debug(String.format("XMapEvent for unmanaged window [%s]", getWindowNameNoLock(e.window)));
				// manage
				Window borderWindow = createBorderWindow(x11.XRootWindow(display, screenIndex),2,Color.RED, Color.ORANGE, new Rectangle(100,100));
				x11.XMapRaised(display, borderWindow);
				managedWindowModel.register(borderWindow, e.window);
				x11.XFlush(display);
				// reparent
				x11.XReparentWindow(display, e.window, borderWindow, 0, 0);
				x11.XChangeSaveSet(display, e.window, X11.SetModeInsert);
				x11.XMapRaised(display, e.window);
				// map
			}
			//
		} finally {
			unlock();
		}
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
			LOG.debug("Bordered window created [{}]", borderWindow);
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
}
