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

package ch.admin.vbs.cube.client.wm.ui.wm;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.ICubeActionListener;
import ch.admin.vbs.cube.client.wm.client.ICubeClient;
import ch.admin.vbs.cube.client.wm.client.IUserInterface;
import ch.admin.vbs.cube.client.wm.client.IVmChangeListener;
import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.client.VmChangeEvent;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.ui.CubeUI.CubeScreen;
import ch.admin.vbs.cube.client.wm.ui.ICubeUI;
import ch.admin.vbs.cube.client.wm.ui.IWindowsControl;
import ch.admin.vbs.cube.client.wm.ui.dialog.ButtonLessDialog;
import ch.admin.vbs.cube.client.wm.ui.dialog.CubeConfirmationDialog;
import ch.admin.vbs.cube.client.wm.ui.dialog.CubeInitialDialog;
import ch.admin.vbs.cube.client.wm.ui.dialog.CubePasswordDialog;
import ch.admin.vbs.cube.client.wm.ui.dialog.CubePasswordDialogListener;
import ch.admin.vbs.cube.client.wm.ui.dialog.CubeWizard;
import ch.admin.vbs.cube.client.wm.ui.x.IWindowManagerCallback;
import ch.admin.vbs.cube.client.wm.ui.x.IXWindowManager;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.IClientFacade;

/**
 * This class is responsible to handle request for UI element (dialog, etc). It
 * ensure that only one dialog is visible (cancel other if needed).
 * 
 * 
 * 
 */
public class WindowManager implements IWindowsControl, IUserInterface, IWindowManagerCallback, IVmChangeListener {
	private static final Color BACKGROUND_COLOR = Color.DARK_GRAY;
	private static final int WINDOW_LOCATION_Y = 25;
	private static final int WINDOW_LOCATION_X = 0;
	private static final int BORDER_SIZE = 5;
	/** Logger */
	private static final ArrayList<Window> EMPTY_WINDOW_LIST = new ArrayList<Window>();
	private static final Logger LOG = LoggerFactory.getLogger(WindowManager.class);
	private static final String VIRTUALMACHINE_WINDOWFMT = "^%s - .*Oracle VM VirtualBox.*$";
	private Pattern windowPatternVirtualMachine = Pattern.compile(String.format(VIRTUALMACHINE_WINDOWFMT, "(.*)"));
	private Object lock = new Object();
	private CubeWizard dialog;
	private HashMap<Long, WindowCachedHandle> managedWindow = new HashMap<Long, WindowCachedHandle>();
	private HashMap<String, VmHandle> cachedVmList = new HashMap<String, VmHandle>();
	private HashMap<String, Window> borderedWindows = new HashMap<String, Window>();
	private VisibleWindows visibleWindows = new VisibleWindows();
	private IXWindowManager xwm;
	private IVmMonitor vmMon;
	private ICubeActionListener cubeActionListener;
	private ICubeClient client;
	private ICubeUI cubeUI;

	public WindowManager() {
	}

	/**
	 * Close current dialog (via Swing-Thread).
	 */
	private void closeCurrentDialog() {
		if (dialog != null) {
			// copy reference on current opened dialog
			final CubeWizard tdial = dialog;
			// use SwingUtilities to perform this action
			// from swing thread.
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					LOG.trace("close dialog [" + tdial.getClass() + "]");
					tdial.setVisible(false);
					tdial.dispose();
				}
			});
			dialog = null;
		}
	}

	@Override
	public void closeDialog() {
		closeCurrentDialog();
	}

	/**
	 * Hide navigation bar and VMs. Make the screen clean (only showing
	 * background). Typically before displaying a dialog.
	 */
	private void hideNavigationBarAndVms() {
		// build a list of x-window we want to hide and show
		ArrayList<Window> hide = new ArrayList<Window>();
		// add all bordered windows (one per screen) into 'hide' list.
		synchronized (lock) {
			for (Entry<String, Window> e : borderedWindows.entrySet()) {
				hide.add(e.getValue());
			}
		}
		// add also NavigationBar frames to 'hide' list
		for (CubeScreen n : cubeUI.getScreens()) {
			LOG.trace("Hide navigation bar [{}]",n.getNavigationBar().getTitle());
			hide.add(getXWindow(n.getNavigationBar()));
		}
		// hide windows
		synchronized (xwm) {
			xwm.showOnlyTheseWindow(hide, EMPTY_WINDOW_LIST);
		}
	}

	/** show navigation frames and bordered windows (containing VMs window). */
	private void showNavigationBarAndVms(boolean raiseNavbar) {
		synchronized (lock) {
			if (dialog != null) {
				LOG.trace("Skip showNavigationBarAndVms because a dialog is opened.");
				return;
			}
			// index visible window's IDs
			Set<String> visibleIds = visibleWindows.getVisibleVmIds();
			// show VM window
			ArrayList<Window> show = new ArrayList<Window>();
			ArrayList<Window> hide = new ArrayList<Window>();
			synchronized (lock) {
				for (Entry<String, Window> e : borderedWindows.entrySet()) {
					if (visibleIds.contains(e.getKey())) {
						LOG.trace("Show bordered windows of vm [{}] since it is in visible windows set", e.getKey());
						show.add(e.getValue());
					} else {
						LOG.trace("Hide bordered windows of vm [{}] since it is NOT in visible windows set", e.getKey());
						hide.add(e.getValue());
					}
				}
			}
			if (raiseNavbar) {
				// add NavigationBar to show list
				for (CubeScreen n : cubeUI.getScreens()) {
					if (n.isActive()) {
						LOG.trace("show NavigationBar");
						show.add(getXWindow(n.getNavigationBar()));
					} else {
						hide.add(getXWindow(n.getNavigationBar()));
					}
				}
			}
			// show windows
			synchronized (xwm) {
				xwm.showOnlyTheseWindow(hide, show);
			}
		}
	}

	// ###############################################
	// Implements IWindowsManagerCallBack
	// ###############################################
	@Override
	public void windowCreated(Window w) {
		// do nothing, since its name may not be initialized
	}

	@Override
	public void windowUpdated(Window w) {
		/**
		 * windowUpdated is called whenever a window attribute is updated. In
		 * our case, we use it when VirtualBox set the window's name of a VM: by
		 * parsing it, we are able to identify to which VM does this window
		 * belongs to.
		 */
		synchronized (managedWindow) {
			// check if window is in cache
			WindowCachedHandle cached = managedWindow.get(w.longValue());
			// if not already caches, check if its name match a VM window's name
			if (cached == null) {
				// fetch window's name
				String windowName = null;
				synchronized (xwm) {
					windowName = xwm.getWindowName(w);
				}
				if (windowName != null) {
					// parse window's name
					Matcher appMx = windowPatternVirtualMachine.matcher(windowName);
					if (appMx.matches()) {
						// This is a VirtualBox Window. create a cache entry
						cached = new WindowCachedHandle(w, appMx.group(1));
						managedWindow.put(w.longValue(), cached);
						/*
						 * re-parent to bordered window (bordered windows are
						 * created in showVms method)
						 */
						cached.borderWindow = borderedWindows.get(cached.vmId);
						if (cached.borderWindow == null) {
							LOG.debug("A new virtual machine window [{}] has been found, but no corresponding bordered window found. Hide window in root window.",w);
							synchronized (xwm) {
								xwm.hideAndReparentToRoot(w);
							}
						} else {
							LOG.debug("A new virtual machine window [{}]  has been found, and matching bordered window has been found, reparent them.",w);
							// re-parent in the corresponding bordered window
							synchronized (xwm) {
								xwm.reparentWindow(cached.borderWindow, w);
							}
						}
					} else {
						LOG.debug("A new window [{}] has been found, but since it is not a virtual machine window, it  will not be managed [{}]", w, windowName);
					}
				} else {
					LOG.trace("A new window [{}] has been found, but since it has not title it will not be managed [{}].", w, windowName);
				}
			} else {
				String windowName = null;
				synchronized (xwm) {
					windowName = xwm.getWindowName(w);
				}
				// window already managed & type determined
				LOG.trace("Window [" + w + "]["+windowName+"] already managed [{}/{}]", cached.window, cached.vmId);
				// 
				
			}
		}
	}

	@Override
	public void windowDestroyed(Window window) {
		synchronized (managedWindow) {
			WindowCachedHandle h = managedWindow.remove(window.longValue());
			if (h == null) {
				LOG.debug("Unmanaged Window destroyed [{}]", window);
			} else {
				LOG.debug("Managed Window [{}/{}] destroyed", h.window, h.vmId);
			}
		}
	}

	/** Cache entry */
	private class WindowCachedHandle {
		public final Window window;
		public Window borderWindow;
		private final String vmId;

		public WindowCachedHandle(Window window, String vmId) {
			this.window = window;
			this.vmId = vmId;
		}
	}

	/**
	 * Creates a new window with a define border. The color of the border
	 * depends of the classification of the vm.
	 * 
	 * @param cubeFrame
	 *            the CubeFrame on which the new border window should be placed
	 * @param vm
	 *            the vm for which the border window will be created
	 * @return a new border window
	 */
	private Window createNewBorderWindow(JFrame cubeFrame, VmHandle h) {
		// create and save border window
		Rectangle bounds = new Rectangle(WINDOW_LOCATION_X, WINDOW_LOCATION_Y, cubeFrame.getBounds().width - WINDOW_LOCATION_X, cubeFrame.getBounds().height
				- WINDOW_LOCATION_Y);
		Color borderColor = BorderColorProvider.getBackgroundColor(vmMon.getVmClassification(h));
		synchronized (xwm) {
			Window win = xwm.createBorderWindow(xwm.findWindowByTitle(cubeFrame.getTitle()), BORDER_SIZE, borderColor, BACKGROUND_COLOR, bounds);
			LOG.debug("createNewBorderWindow for jframe [{}] ==> window[{}]", cubeFrame.getTitle(), win);
			return win;
		}
	}

	// ###############################################
	// Implements IWindowsControl
	// ###############################################
	@Override
	public void moveVmWindow(VmHandle h, String monitorIdxBeforeUpdate) {
		if (h.getMonitorId().equals(monitorIdxBeforeUpdate)) {
			// not moved
			LOG.debug("Skip moving VM Window [{}] to monitor [{}]", h.getVmId(), h.getMonitorId());
			return;
		}
		// get new parent frame
		LOG.debug("Move VM Window [{}] to monitor [{}]", h.getVmId(), h.getMonitorId());
		Window borderWindow = null;
		synchronized (lock) {
			borderWindow = borderedWindows.get(h.getVmId());
		}
		// reset visible window on the 'old' screen if it was showing our VM.
		visibleWindows.set(h.getMonitorId(), h);
		// reparent bordered windows
		CubeScreen scr = cubeUI.getScreen(h.getMonitorId());
		JFrame frame = scr.getBackgroundFrame();
		Rectangle bounds = new Rectangle(WINDOW_LOCATION_X, WINDOW_LOCATION_Y, frame.getBounds().width - WINDOW_LOCATION_X - 2 * BORDER_SIZE,
				frame.getBounds().height - WINDOW_LOCATION_Y - 2 * BORDER_SIZE);
		LOG.trace("Move target window [{}][{}]", getXWindow(frame), borderWindow);
		synchronized (xwm) {
			xwm.reparentWindowAndResize(getXWindow(frame), borderWindow, bounds);
		}
		// set in foreground
		scr.getNavigationBar().selectTab(h);
	}

	/**
	 * Get corresponding XWindow for a given JFrame object (java). Use frame
	 * name in order to find the right XWindow.
	 */
	private final Window getXWindow(JFrame jframe) {
		if (jframe == null) {
			throw new NullPointerException("Argument parentFrame must be none-null");
		}
		Window w;
		synchronized (xwm) {
			w = xwm.findWindowByTitle(jframe.getTitle());
			if (w == null) {
				LOG.error("Not XWindow found for window [{}]", jframe.getTitle());
			}
		}
		return w;
	}

	@Override
	public void showVmWindow(VmHandle h) {
		/*
		 * Called by NavigationTabs -> stateChanged
		 */
		visibleWindows.set(h.getMonitorId(), h);
		/*
		 * do not show navigation bars or it will hide pop-up menu.
		 */
		showNavigationBarAndVms(false);
	}

	@Override
	public void hideAllVmWindows(String monitorId) {
		LOG.trace("hideAllVmWindows({})",monitorId);
		visibleWindows.set(monitorId, null);
		showNavigationBarAndVms(false);
	}

	// ###############################################
	// Implements IVmChangelistener
	// ###############################################
	@Override
	public void allVmsChanged() {
		/**
		 * VM list has changed. We have must ensure that all VM already has its
		 * bordered window ready (with the right border color). It implies
		 * creating bordered windows and eventually re-parent an existing
		 * VirtualBox window to it and removing not more used bordered windows.
		 */
		synchronized (lock) {
			// index VmHanles by VM's Ids
			HashMap<String, VmHandle> handleIndex = new HashMap<String, VmHandle>();
			for (VmHandle h : client.listVms()) {
				handleIndex.put(h.getVmId(), h);
			}
			Collection<VmHandle> vmsToAdd = findVmsToAdd(cachedVmList.values(), handleIndex.values());
			Collection<VmHandle> vmsToRemove = findVmToRemove(cachedVmList.values(), handleIndex.values());
			// remove unused bordered window
			for (VmHandle h : vmsToRemove) {
				LOG.trace("Remove bordered window for VM [" + h.getVmId() + "] on monitor [" + h.getMonitorId() + "]");
				// remove from local list
				Window borderedWindow = borderedWindows.remove(h.getVmId());
				WindowCachedHandle mngWin = findManagedWindowByBorderedWindow(borderedWindow);
				synchronized (xwm) {
					/*
					 * Unmap and re-parent VirtualBox child window if present.
					 * If we do not do it, VirtualBox will abort the virtual
					 * machine a soon as we deleted its parent window.
					 */
					if (mngWin != null) {
						LOG.debug("Hide un-used VM window [{}] [{}]", mngWin.vmId, mngWin.window);
						xwm.hideAndReparentToRoot(mngWin.window);
						mngWin.borderWindow = null;
					}
					// remove bordered window from XServer
					xwm.removeWindow(borderedWindow);
				}
			}
			// setup bordered window for each VM
			for (VmHandle h : vmsToAdd) {
				LOG.debug("Create bordered window for VM [" + h.getVmId() + "] on monitor [" + h.getMonitorId() + "]");
				Window brdWin = createNewBorderWindow(getDefaultParentFrame(), h);
				borderedWindows.put(h.getVmId(), brdWin);
				// look if a VirtualBox window is already managed for this
				// VM and re-parent its virtual machine window.
				synchronized (managedWindow) {
					for (WindowCachedHandle c : managedWindow.values()) {
						if (c.vmId.equals(h.getVmId())) {
							if (c.borderWindow != null) {
								/**
								 * This should never append. It would implies
								 * that another bordered window already exists
								 * in the system for this managed window.
								 */
								LOG.error("CRITICAL: BorderedWindow Conflitct. Contact dev to debug this issue asap.");
								return;
							}
							c.borderWindow = brdWin;
							LOG.debug("Managed Window found for VM [{}]. Re-parent virtual machine window to this bordered window", h.getVmId());
							synchronized (xwm) {
								xwm.reparentWindow(c.borderWindow, c.window);
							}
							break;
						}
					}
				}
			}
			cachedVmList = handleIndex;
		}
	}

	/** Find WindowCachedHandle object that reference this bordered window. */
	private WindowCachedHandle findManagedWindowByBorderedWindow(Window borderedWindow) {
		synchronized (managedWindow) {
			for (WindowCachedHandle c : managedWindow.values()) {
				if (c.borderWindow != null && borderedWindow.longValue() == c.borderWindow.longValue()) {
					return c;
				}
			}
		}
		return null;
	}

	/** Diff both list and return VM add in the new list. */
	private final Collection<VmHandle> findVmsToAdd(Collection<VmHandle> oldList, Collection<VmHandle> newList) {
		ArrayList<VmHandle> result = new ArrayList<VmHandle>(newList);
		result.removeAll(oldList);
		return result;
	}

	/** Diff both list and return VM removed in the new list. */
	private final Collection<VmHandle> findVmToRemove(Collection<VmHandle> oldList, Collection<VmHandle> newList) {
		ArrayList<VmHandle> result = new ArrayList<VmHandle>(oldList);
		result.removeAll(newList);
		return result;
	}

	@Override
	public void vmChanged(VmChangeEvent event) {
		// nothing to do.
	}

	// ###############################################
	// Implements IUserInterface
	// ###############################################
	@Override
	public void showMessageDialog(String message, int options) {
		LOG.debug("showMessageDialog()");
		synchronized (lock) {
			closeCurrentDialog();
			hideNavigationBarAndVms();
			// dialog is non-blocking
			if (options == IClientFacade.OPTION_SHUTDOWN) {
				// only one option : OPTION_SHUTDOWN
				final CubeInitialDialog dial = new CubeInitialDialog(getDefaultParentFrame(), message, cubeActionListener);
				dialog = dial;
				swingOpen(dial);
			} else {
				// default
				final ButtonLessDialog msgdialog = new ButtonLessDialog(getDefaultParentFrame(), message);
				dialog = msgdialog;
				swingOpen(msgdialog);
			}
		}
	}

	private void swingOpen(final CubeWizard msgdialog) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				LOG.trace("exec  [displayWizard] [{}]", msgdialog);
				msgdialog.displayWizard();
			}
		});
	}

	@Override
	public void showPinDialog(final String additionalMessage, final String requestId) {
		LOG.debug("showPinDialog()");
		synchronized (lock) {
			LOG.trace("enter [showPinDialog] [{}]", additionalMessage);
			closeCurrentDialog();
			hideNavigationBarAndVms();
			// create dialog (non-blocking)
			final CubePasswordDialog passwordDialog = new CubePasswordDialog(getDefaultParentFrame());
			passwordDialog.addPasswordDialogListener(new CubePasswordDialogListener() {
				@Override
				public void quit(final char[] password) {
					cubeActionListener.enteredPassword(password, requestId);
				}
			});
			// set as active dialog
			dialog = passwordDialog;
			// open dialog
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					LOG.debug("exec  [displayWizard] [{}]", passwordDialog.getClass());
					passwordDialog.displayWizard(additionalMessage);
				}
			});
			LOG.trace("exit  [showPinDialog] [{}]", additionalMessage);
		}
	}

	@Override
	public void showTransferDialog(VmHandle h, RelativeFile file) {
		LOG.warn("[showTransferDialog()] not implemented");
	}

	@Override
	public void showConfirmationDialog(final String messageKey, final String requestId) {
		LOG.debug("showConfirmationDialog()");
		synchronized (lock) {
			closeCurrentDialog();
			hideNavigationBarAndVms();
			// create dialog
			final CubeConfirmationDialog dial = new CubeConfirmationDialog(getDefaultParentFrame(), messageKey, CubeConfirmationDialog.TYPE_CANCEL_YES);
			// set as active dialo
			dialog = dial;
			// display dialog
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					LOG.debug("exec  [showConfirmationDialog] [{}]", messageKey);
					dial.displayWizard();
					// return result
					cubeActionListener.enteredConfirmation(dial.getDialogResult(), requestId);
				}
			});
		}
	}

	

	private JFrame getDefaultParentFrame() {
		return cubeUI.getDefaultScreen().getBackgroundFrame();
	}

	@Override
	public void showVms() {
		/*
		 * Called by ClientFacade in order to display workspace when a dialog has been closed.
		 */
		LOG.trace("ShowVMs()");
		synchronized (lock) {
			// close all dialogs
			closeCurrentDialog();
			// ensure that navigation bar are visible
			showNavigationBarAndVms(true);
		}
	}

	@Override
	public void refresh() {
		LOG.trace("refresh()");
		synchronized (lock) {
			if (dialog == null) {
				LOG.trace("Refresh WM: show VMs + navbar");
				showNavigationBarAndVms(true);
			} else {
				LOG.trace("Refresh WM: show Dialog");
				ArrayList<Window> show = new ArrayList<Window>();
				ArrayList<Window> hide = new ArrayList<Window>();
				// hide VMs' windows
				synchronized (lock) {
					for (Entry<String, Window> e : borderedWindows.entrySet()) {
						hide.add(e.getValue());
					}
				}
				// hide NavigationBar
				for (CubeScreen n : cubeUI.getScreens()) {
					hide.add(getXWindow(n.getNavigationBar()));
				}
				// show dialog
				synchronized (xwm) {
					Window w = xwm.findWindowByTitle(CubeWizard.WIZARD_WINDOW_TITLE);
					if (w != null) {
						show.add(w);
					}
				}
				// hide windows
				synchronized (xwm) {
					xwm.showOnlyTheseWindow(hide, show);
				}
			}
		}
	}

	private class VisibleWindows {
		private HashMap<String, VmHandle> layout = new HashMap<String, VmHandle>();
		private HashMap<String, String> ilayout = new HashMap<String, String>();

		private void set(String monitorId, VmHandle h) {
			synchronized (layout) {
				if (h == null) {
					// if VmHanlde is null.. see if a VM was on this monitor
					h = layout.remove(monitorId);
					if (h != null) {
						// remove old VM
						ilayout.remove(h.getVmId());
					}
				} else {
					// eventually remove handler from another monitor
					String oldMon = ilayout.remove(h.getVmId());
					if (oldMon != null) {
						layout.remove(oldMon);
					}
					// put handler in new monitor
					layout.put(monitorId, h);
					ilayout.put(h.getVmId(), monitorId);
				}
				if (LOG.isTraceEnabled()) {
					LOG.trace("VisibleWindows.set()");
					for (Entry<String, VmHandle> e : layout.entrySet()) {
						LOG.trace("- Visible window: [{}][{}]", e.getKey(), e.getValue());
					}
				}
			}
		}

		private Set<String> getVisibleVmIds() {
			HashSet<String> ids = new HashSet<String>();
			synchronized (layout) {
				for (VmHandle h : layout.values()) {
					ids.add(h.getVmId());
				}
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("VisibleWindows.getVisibleVmIds()");
				for (Entry<String, VmHandle> e : layout.entrySet()) {
					LOG.trace("- Visible window: [{}][{}]", e.getKey(), e.getValue());
				}
			}
			return ids;
		}
	}

	// ###############################################
	// Injections
	// ###############################################
	public void setup(ICubeClient client, ICubeActionListener cubeActionListener, IVmMonitor vmMon, IXWindowManager xwm, ICubeUI cubeUI) {
		this.client = client;
		this.cubeUI = cubeUI;
		client.addListener(this);
		this.xwm = xwm;
		xwm.setWindowManagerCallBack(this);
		this.vmMon = vmMon;
		// if (osdMgmt != null) {
		// osdMgmt.setVmMon(vmMon);
		// }
		this.cubeActionListener = cubeActionListener;
	}
}
