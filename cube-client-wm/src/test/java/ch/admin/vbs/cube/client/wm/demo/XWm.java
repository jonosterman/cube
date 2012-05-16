package ch.admin.vbs.cube.client.wm.demo;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.demo.XWm.IClientWindowLayout.WinType;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Display;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.WindowByReference;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XEvent;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.XTextProperty;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class XWm {
	private static final Logger LOG = LoggerFactory.getLogger(XWm.class);
	protected static final long CHECKING_INTERVAL_FOR_NEW_EVENTS = 1000;

	private enum Arch {
		OS_32_BIT, OS_64_BIT
	}

	public static void main(String[] args) {
		XWm xwm = new XWm();
		xwm.init();
	}

	private X11 x11;
	private String displayName;
	private Display display;
	private int xScreenIndex;
	private Arch osArchitectur;
	private IClientWindowLayout clientLayout = new HardCodedWindowLayout("- Oracle VM VirtualBox");
	private Thread eventThread;
	private boolean destroyed;
	private Lock lock = new ReentrantLock();
	private Window target;

	private void init() {
		// find out if we run a 64bits system
		detectOsArchitecture();
		// get x11 instance & EWMH
		x11 = X11.INSTANCE;
		// find out display name and screen index
		displayName = System.getenv("DISPLAY");
		if (displayName != null) {
			// displayName is something like this ":0.0"
			int dotIndex = displayName.indexOf(".");
			if (dotIndex < 0) {
				xScreenIndex = 0;
			} else {
				xScreenIndex = Integer.parseInt(displayName.substring(dotIndex + 1));
			}
		} else {
			displayName = ":0.0";
			xScreenIndex = 0;
		}
		// thread
		openDisplay();
		eventThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					XEvent event = new XEvent();
					while (!destroyed) {
						try {
							// check for new events
							lock.lock();
							while (x11.XPending(display) > 0) {
								x11.XNextEvent(display, event);
								processEvent(event);
							}
							lock.unlock();
							Thread.sleep(CHECKING_INTERVAL_FOR_NEW_EVENTS);
						} catch (Exception e) {
							LOG.error("Error during catching events", e);
						}
					}
					LOG.debug("Quit event thread.");					
				} catch (Exception e) {
					LOG.error("XEvent Thread Failed");
				}
			}
		});
		eventThread.start();
		//
		lock.lock();		
		// initial : check all already opened windows
		LOG.info("---------------------------");
		target = debug_selectTargetWindow();
		System.err.println("Target Window: " + target);
		while (target ==null) {
			target = debug_selectTargetWindow();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
			}
		}
		// manage window
		x11.XSetCloseDownMode(display, X11.RetainPermanent);
		final Window borderWindow = x11.XCreateSimpleWindow(//
				display, //
				x11.XRootWindow(display, xScreenIndex), //
				300, 300, //
				644, 484, //
				2, //
				Color.RED.getRGB(), Color.CYAN.getRGB());
		LOG.debug("Bordered window created [{}]", borderWindow);
		// reparent
		System.err.println("target window: [" + target + "]");
		//EWMH ewmh = new EWMH(display);
		// ewmh.readIntProperty(target, x11.XInternAtom(display, "", false));
		// if (0 == System.currentTimeMillis()) {
		// unmap so the actual WM (if any) will unmanage it
		x11.XUnmapWindow(display, target);
		x11.XFlush(display);
		// re-order as child of our border-window
		int reparent = x11.XReparentWindow(display, target, borderWindow, 2, 2);
		LOG.debug("Reparent result [{}]",reparent);
		x11.XSelectInput(display, target, new NativeLong(/*X11.SubstructureRedirectMask |*/ X11.ResizeRedirectMask | X11.EnterWindowMask | X11.SubstructureNotifyMask | X11.StructureNotifyMask));
		//x11.XChangeSaveSet(display, target, X11.SetModeInsert);
		x11.XFlush(display);
		LOG.debug("target window re-parented");
		// map both client and border-window
		x11.XMapWindow(display, borderWindow);
		x11.XMapWindow(display, target);
		x11.XFlush(display);
		
		// play with it
		LOG.debug("bordered window mapped");
		
		JPanel panel = new JPanel();
		JFrame jframe = new JFrame("control");
		jframe.setContentPane(panel);
		//
		final JTextField width = new JTextField("640");
		final JTextField height = new JTextField("480");
		panel.add(width);
		panel.add(height);
		JButton apply = new JButton("Apply");
		panel.add(apply);
		apply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.err.println("-------------------");
				int w = 640;
				int h = 480;
				try {
					w = Integer.parseInt(width.getText());
					h = Integer.parseInt(height.getText());
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				// 
				lock.lock();
				int bm = x11.XMoveResizeWindow(display, borderWindow, 300, 300, w+4, h+4);
				x11.XFlush(display);
				LOG.debug("bordered move result [{}]", bm);				
				x11.XMoveResizeWindow(display, target, 2, 2, w, h);
				x11.XFlush(display);
				lock.unlock();
			}
		});
		JButton move = new JButton("Move");
		panel.add(move);
		move.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.err.println("-------------------");
				lock.lock();
				x11.XMoveWindow(display, target, 2, 2);
				x11.XFlush(display);
				lock.unlock();
			}
		});
		JButton close = new JButton("Close");
		panel.add(close);
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.err.println("-------------------");
				lock.lock();
				x11.XUnmapWindow(display, borderWindow);
				x11.XReparentWindow(display, target, x11.XRootWindow(display, xScreenIndex), 100, 100);
				// }
				destroyed = true;
				closeDisplay();
				lock.unlock();
				LOG.info("---------------------------");
				System.exit(0);
			}
		});
		lock.unlock();
		//
		jframe.pack();
		jframe.setVisible(true);
		

//		//
//		for (double i = 0; i < 2 * Math.PI; i += 0.5) {
//			int w = (int) (600+ 100 + 100 * Math.abs(Math.cos(i * 1.2)));
//			int h = (int) (480+ 100 + 100 * Math.abs(Math.cos(i * 1.2)));
//			int x = (int) (1000 + (Math.sin(i) * 200)-w/2);
//			int y = (int) (1000 + (Math.cos(i) * 200)-h/2);
//			//
//			x11.XMoveResizeWindow(display, borderWindow, x, y, w+4, h+4);
//			x11.XMoveResizeWindow(display, target, 2, 2, w, h);
//			x11.XFlush(display);
//			try {
//				System.out.println("...................................................");
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//			}
//		}
		//
		
	}

	private Window debug_selectTargetWindow() {
		Window rootWindow = x11.XRootWindow(display, xScreenIndex);
		return debug_selectTargetWindowRec(rootWindow);
	}

	public static interface IClientWindowLayout {
		enum WinType { VM_WINDOW, OTHER }
		WinType testWindowTitle(String title);
	}

	private class HardCodedWindowLayout implements IClientWindowLayout {
		private final String suffix;

		public HardCodedWindowLayout(String suffix) {
			this.suffix = suffix;
		}

		@Override
		public WinType testWindowTitle(String title) {
			if (title != null && title.endsWith(suffix)) {
				return WinType.VM_WINDOW;
			} else {
				return WinType.OTHER;
			}
		}
	}

	private Window debug_selectTargetWindowRec(Window window) {
		//
		XTextProperty windowTitle = new XTextProperty();
		x11.XFetchName(display, window, windowTitle);
		if (clientLayout.testWindowTitle(windowTitle.value) == WinType.VM_WINDOW) {
			System.err.printf("Target window found [%s][%s]\n", window, windowTitle.value);
			LOG.debug("Target window found [{}][{}]", window, windowTitle.value);
			return window;
		} else {
			LOG.debug("Skip [{}]", windowTitle.value);
		}
		// rec
		long[] childrenWindowIdArray = getChildrenList(display, window);
		for (long windowId : childrenWindowIdArray) {
			Window child = new Window(windowId);
			Window nx = debug_selectTargetWindowRec(child);
			if (nx != null)
				return nx;
		}
		return null;
	}

	private void openDisplay() {
		display = x11.XOpenDisplay(displayName);
	}

	private void closeDisplay() {
		x11.XCloseDisplay(display);
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
		// get all window id's
		if (childrenCount.getValue() > 0) {
			// the id's size is architecture dependent
			switch (osArchitectur) {
			case OS_64_BIT:
				childrenWindowIdArray = childrenPtr.getValue().getLongArray(0, childrenCount.getValue());
				break;
			case OS_32_BIT:
				int[] intChildrenWindowIdArray = childrenPtr.getValue().getIntArray(0, childrenCount.getValue());
				childrenWindowIdArray = new long[intChildrenWindowIdArray.length];
				int index = 0;
				for (int windowId : intChildrenWindowIdArray) {
					childrenWindowIdArray[index] = windowId;
					++index;
				}
				break;
			}
		}
		return childrenWindowIdArray;
	}

	private void detectOsArchitecture() {
		String osBits = System.getProperty("os.arch");
		LOG.debug("Detected architecture [{}]", osBits);
		if ("amd64".equals(osBits)) {
			osArchitectur = Arch.OS_64_BIT;
		} else {
			osArchitectur = Arch.OS_32_BIT;
		}
	}
	
	private void processEvent(XEvent event) {
		if (!destroyed) {
			LOG.debug("Got an XEvent [{}]", X11.XEventName.getEventName(event.type));
			switch (event.type) {	
			case X11.ResizeRequest:
				X11.XResizeRequestEvent resizeRequest = (X11.XResizeRequestEvent) event.getTypedValue(X11.XResizeRequestEvent.class);			
				System.err.println("Event [ResizeRequest] : (" + resizeRequest.width +"x"+ resizeRequest.height+")");
				break;
			case X11.ConfigureNotify:
				X11.XConfigureEvent confNotify = (X11.XConfigureEvent) event.getTypedValue(X11.XConfigureEvent.class);			
				System.err.println("Event [ConfigureNotify] : (" + confNotify.x +":"+ confNotify.y +") ("+ confNotify.width +"x"+ confNotify.height+")");
				// ensure it is moved at hte right place
				if (confNotify.x != 2 || confNotify.y !=2) {
					System.err.println(">> force position to (2:2)..");
					lock.lock();
					x11.XMoveWindow(display, confNotify.window, 2, 2);
					lock.unlock();
				}
				
				break;
			default:
				System.err.println("Ignore XEvent ["+event.type+"]");
				break;
			}
		}
	}
}
