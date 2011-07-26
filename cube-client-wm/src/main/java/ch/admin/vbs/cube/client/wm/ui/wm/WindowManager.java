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
import ch.admin.vbs.cube.client.wm.client.IXWindowManager;
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
import ch.admin.vbs.cube.client.wm.ui.dialog.UsbChooserDialog;
import ch.admin.vbs.cube.client.wm.ui.tabs.NavigationBar;
import ch.admin.vbs.cube.client.wm.ui.x.IWindowManagerCallback;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;
import ch.admin.vbs.cube.client.wm.ui.x.imp.XWindowManager;
import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.IClientFacade;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntryList;

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
	private static final Logger LOG = LoggerFactory.getLogger(WindowManager.class);
	private Object lock = new Object();
	private CubeWizard dialog;
	private HashMap<Long, WindowCachedHandle> managedWindow = new HashMap<Long, WindowCachedHandle>();
	private HashMap<String, VmHandle> cachedVmList = new HashMap<String, VmHandle>();
	private HashMap<JFrame, Window> cachedWindows = new HashMap<JFrame, Window>();
	private HashMap<String, Window> borderedWindows = new HashMap<String, Window>();
	private Pattern windowPatternVirtualMachine = Pattern.compile("^(.*) - .*Oracle VM VirtualBox.*$");
	private Pattern windowPatternNavigationBar = Pattern.compile("^" + NavigationBar.FRAME_TITLEPREFIX + "(.*+)$");
	private HashMap<String, VmHandle> visibleWindows = new HashMap<String, VmHandle>();
	private NavigationBar[] navbarFrames;
	private JFrame[] parentFrames;
	// private OsdFrameManager osdMgmt;
	//
	private IXWindowManager xwm;
	private IVmMonitor vmMon;
	private ICubeActionListener cubeActionListener;
	private ICubeClient client;
	private ICubeUI cubeUI;

	public WindowManager() {
	}

	private void closeCurrentDialog() {
		if (dialog != null) {
			final CubeWizard tdial = dialog;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					LOG.debug("exec  [close/dispose] [{}]", tdial);
					tdial.setVisible(false);
					tdial.dispose();
				}
			});
			LOG.debug("close dialog [" + dialog + "]");
			dialog = null;
		}
	}

	@Override
	public void closeDialog() {
		closeCurrentDialog();
	}

	private void hideNavigationBarAndVms() {
		ArrayList<Window> show = new ArrayList<Window>();
		ArrayList<Window> hide = new ArrayList<Window>();
		// add all VMs window in hide list
		synchronized (lock) {
			for (Entry<String, Window> e : borderedWindows.entrySet()) {
				hide.add(e.getValue());
			}
		}
		// add NavigationBar to hide list
		for (CubeScreen n : cubeUI.getScreens()) {
			hide.add(getCachedWindow(n.getNavigationBar()));
		}
		// hide windows
		synchronized (xwm) {
			xwm.showOnlyTheseWindow(hide, show);
		}
		// hide OSD
		// osdMgmt.hideAll();
	}

	private void showNavigationBarAndVms() {
		synchronized (lock) {
			// index visible window's IDs
			HashSet<String> visibleIds = new HashSet<String>();
			synchronized (visibleWindows) {
				LOG.debug("visible windows [{}]", visibleWindows.size());
				for (VmHandle vh : visibleWindows.values()) {
					visibleIds.add(vh.getVmId());
					LOG.debug("visible windows [{}]", vh.getVmId());
				}
			}
			// show VM window
			ArrayList<Window> show = new ArrayList<Window>();
			ArrayList<Window> hide = new ArrayList<Window>();
			synchronized (lock) {
				for (Entry<String, Window> e : borderedWindows.entrySet()) {
					if (visibleIds.contains(e.getKey())) {
						show.add(e.getValue());
					} else {
						hide.add(e.getValue());
					}
				}
			}
			// add NavigationBar to show list
			for (CubeScreen n : cubeUI.getScreens()) {
				if (n.isActive()) {
					show.add(getCachedWindow(n.getNavigationBar()));
				} else {
					hide.add(getCachedWindow(n.getNavigationBar()));
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
		WindowCachedHandle h = null;
		synchronized (managedWindow) {
			h = managedWindow.get(w.longValue());
			// lazy initialized
			if (h == null) {
				h = new WindowCachedHandle();
				synchronized (xwm) {
					h.name = xwm.getWindowName(w);
				}
				if (h.name == null) {
					h.name = "";
				}
				h.type = null;
				managedWindow.put(w.longValue(), h);
			}
		}
		// update type
		if (h.type == null) {
			// create handle
			h = new WindowCachedHandle();
			synchronized (xwm) {
				h.name = xwm.getWindowName(w);
			}
			if (h.name == null) {
				h.name = "";
			}
			// determine window type
			Matcher m = windowPatternVirtualMachine.matcher(h.name);
			if (m.matches()) {
				// this is a VirtualBox Window
				h.vmId = m.group(1);
				h.type = WindowType.VirtualMachine;
				LOG.debug("Window [VirtualBox] vmId[{}] newly managed", h.vmId);
				// reparent to bordered window (bordered windows are created in
				// showVms method)
				Window win = borderedWindows.get(h.vmId);
				if (win == null) {
					LOG.error("Failed to find corresponding bordered window");
				} else {
					h.borderWindow = win;
					synchronized (xwm) {
						xwm.findAndBindWindowByNamePattern(h.vmId, h.name, h.borderWindow);
					}
				}
			}
			m = windowPatternNavigationBar.matcher(h.name);
			if (h.type == null && m.matches()) {
				// this is a NavigationBar Window
				h.displayId = m.group(1);
				h.type = WindowType.NavigationBar;
				LOG.debug("Window [NavigationBar] displayId[{}] newly managed", h.displayId);
			}
			if (h.type == null && h.name.length() > 0) {
				// Other
				h.type = WindowType.OTHER;
				LOG.debug("Window [Other/" + h.name + "] newly managed", h.displayId);
			}
		} else {
			// window already managed & type determined
			LOG.debug("Window [{}/{}] already managed", h.type, h.name);
		}
	}

	@Override
	public void windowDestroyed(Window window) {
		synchronized (managedWindow) {
			WindowCachedHandle h = managedWindow.get(window.longValue());
			if (h == null) {
				// LOG.debug("Unmanaged Window destroyed");
			} else {
				// LOG.debug("Managed Window [{}/{}] destroyed", h.type,
				// h.name);
				managedWindow.remove(window.longValue());
			}
		}
	}

	private enum WindowType {
		VirtualMachine, NavigationBar, OTHER
	}

	private class WindowCachedHandle {
		public Window borderWindow;
		private WindowType type;
		private String name;
		// in case of VirtualMachine
		private String vmId;
		// in case of NavigationBar
		private String displayId;
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
			return xwm.createBorderWindow(xwm.findWindowByNamePattern(cubeFrame.getTitle()), BORDER_SIZE, borderColor, BACKGROUND_COLOR, bounds);
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
		// reparent bordered windows
		CubeScreen scr = cubeUI.getScreen(h.getMonitorId());
		JFrame frame = scr.getBackgroundFrame();
		Rectangle bounds = new Rectangle(WINDOW_LOCATION_X, WINDOW_LOCATION_Y, frame.getBounds().width - WINDOW_LOCATION_X - 2 * BORDER_SIZE,
				frame.getBounds().height - WINDOW_LOCATION_Y - 2 * BORDER_SIZE);
		LOG.debug("Move target window [{}][{}]", getCachedWindow(frame), borderWindow);
		synchronized (xwm) {
			xwm.reparentWindowAndResize(getCachedWindow(frame), borderWindow, bounds);
		}// set in foreground
		scr.getNavigationBar().selectTab(h);
	}

	/**
	 * Since some window (navigation,parents) never change. It make sense to
	 * cache them instead of looking them up each time (using
	 * findWindowByNamePattern()) .
	 */
	private final Window getCachedWindow(JFrame parentFrame) {
		if (parentFrame == null) {
			throw new NullPointerException("Argument parentFrame must be none-null");
		}
		synchronized (cachedWindows) {
			Window w = cachedWindows.get(parentFrame);
			if (w == null) {
				synchronized (xwm) {
					w = xwm.findWindowByNamePattern(parentFrame.getTitle());
					if (w == null) {
						LOG.error("Not XWindow found for window [{}]", parentFrame.getTitle());
					}
				}
				cachedWindows.put(parentFrame, w);
			}
			return w;
		}
	}

	@Override
	public void showVmWindow(VmHandle vm) {
		// update visibleWindows map
		HashSet<String> visibleIds = new HashSet<String>();
		synchronized (visibleWindows) {
			if (vm != null) {
				visibleWindows.put(vm.getMonitorId(), vm);
			}
			// update osds
			// osdMgmt.update(visibleWindows.values());
			// do not show window if a dialog is displayed
			if (dialog != null)
				return;
			// index visible window's IDs
			for (VmHandle vh : visibleWindows.values()) {
				visibleIds.add(vh.getVmId());
			}
		}
		// show VM window
		ArrayList<Window> show = new ArrayList<Window>();
		ArrayList<Window> hide = new ArrayList<Window>();
		synchronized (lock) {
			for (Entry<String, Window> e : borderedWindows.entrySet()) {
				if (visibleIds.contains(e.getKey())) {
					show.add(e.getValue());
				} else {
					hide.add(e.getValue());
				}
			}
		}
		synchronized (xwm) {
			xwm.showOnlyTheseWindow(hide, show);
		}// show OSD
			// osdMgmt.showOsdFrames();
	}

	@Override
	public void hideAllVmWindows(String monitorId) {
		LOG.debug("Hide all VMs windows");
		// update visibleWindows map
		HashSet<String> visibleIds = new HashSet<String>();
		synchronized (visibleWindows) {
			visibleWindows.remove(monitorId);
			// do not show/hide window if a dialog is displayed
			if (dialog != null)
				return;
			// index visible window's IDs
			for (VmHandle vh : visibleWindows.values()) {
				visibleIds.add(vh.getVmId());
			}
		}
		// show VM window
		ArrayList<Window> show = new ArrayList<Window>();
		ArrayList<Window> hide = new ArrayList<Window>();
		synchronized (lock) {
			for (Entry<String, Window> e : borderedWindows.entrySet()) {
				if (visibleIds.contains(e.getKey())) {
					show.add(e.getValue());
				} else {
					hide.add(e.getValue());
				}
			}
		}
		xwm.showOnlyTheseWindow(hide, show);
	}

	// ###############################################
	// Implements IVmChangelistener
	// ###############################################
	@Override
	public void allVmsChanged() {
		/**
		 * VM list has changed. We have to create bordered windows for new VMs
		 * and dispose bordered windows for VM that are no more present in the
		 * list.
		 */
		synchronized (lock) {
			// refresh cached list of VM
			HashMap<String, VmHandle> nCache = new HashMap<String, VmHandle>();
			for (VmHandle h : client.listVms()) {
				nCache.put(h.getVmId(), h);
			}
			// sync border windows: for each VM we will ensure that a X window
			// is present. This X Window have a colored border that match the VM
			// classification. Later, the VM window will be re-parented to it.
			synchronized (lock) {
				Collection<VmHandle> vmsToAdd = findVmsToAdd(cachedVmList.values(), nCache.values());
				Collection<VmHandle> vmsToRemove = findVmToRemove(cachedVmList.values(), nCache.values());
				// remove old
				for (VmHandle h : vmsToRemove) {
					Window w = borderedWindows.remove(h.getVmId());
					synchronized (xwm) {
						xwm.removeWindow(w);
					}
				}
				// add new
				for (VmHandle h : vmsToAdd) {
					Window w = createNewBorderWindow(getDefaultParentFrame(), h);
					borderedWindows.put(h.getVmId(), w);
				}
			}
			cachedVmList = nCache;
		}
	};

	private final Collection<VmHandle> findVmsToAdd(Collection<VmHandle> oldList, Collection<VmHandle> newList) {
		ArrayList<VmHandle> result = new ArrayList<VmHandle>(newList);
		result.removeAll(oldList);
		return result;
	}

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
		synchronized (lock) {
			LOG.debug("enter [showMessageDialog] [{}][{}]", message, options);
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
			LOG.debug("exit  [showMessageDialog] [{}]", message);
		}
	}

	private void swingOpen(final CubeWizard msgdialog) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				LOG.debug("exec  [displayWizard] [{}]", msgdialog);
				msgdialog.displayWizard();
			}
		});
	}

	@Override
	public void showPinDialog(final String additionalMessage, final String requestId) {
		synchronized (lock) {
			LOG.debug("enter [showPinDialog] [{}]", additionalMessage);
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
					LOG.debug("exec  [displayWizard] [{}]", passwordDialog);
					passwordDialog.displayWizard(additionalMessage);
				}
			});
			LOG.debug("exit  [showPinDialog] [{}]", additionalMessage);
		}
	}

	@Override
	public void showTransferDialog(VmHandle h, RelativeFile file) {
		LOG.info("[showTransferDialog()] not implemented");
	}

	@Override
	public void showConfirmationDialog(final String messageKey, final String requestId) {
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

	@Override
	public void showUsbDeviceDialog(final String messageKey, UsbDeviceEntryList list, final String requestId) {
		synchronized (lock) {
			LOG.debug("enter [showVmChooser] [{}]", messageKey);
			closeCurrentDialog();
			hideNavigationBarAndVms();
			// create dialog
			final UsbChooserDialog dial = new UsbChooserDialog(getDefaultParentFrame(), messageKey, vmMon, list);
			// set as active dialog
			dialog = dial;
			// display dialog
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					LOG.debug("exec  [showUsbDeviceDialog] [{}]", messageKey);
					dial.displayWizard();
					// return result
					cubeActionListener.enteredUsbDevice(dial.getSelection(), requestId);
				}
			});
		}
	}

	private JFrame getDefaultParentFrame() {
		return cubeUI.getDefaultScreen().getBackgroundFrame();
	}

	@Override
	public void showVms() {
		synchronized (lock) {
			// close all dialogs
			closeCurrentDialog();
			// ensure that navigation bar are visible
			showNavigationBarAndVms();
			//
			// osdMgmt.showOsdFrames();
		}
	}

	@Override
	public void refresh() {
		synchronized (lock) {
			if (dialog == null) {
				LOG.debug("Refresh WM: show VMs");
				showNavigationBarAndVms();
			} else {
				LOG.debug("Refresh WM: show Dialog");
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
					hide.add(getCachedWindow(n.getNavigationBar()));
				}
				// show dialog
				synchronized (xwm) {
					Window w = xwm.findWindowByNamePattern(CubeWizard.WIZARD_WINDOW_TITLE);
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

	// ###############################################
	// Getter/setter methods
	// ###############################################
	// public void setParentFrame(JFrame[] frames) {
	// this.parentFrames = frames;
	// // init OSD frames
	// if (osdMgmt == null) {
	// OsdFrame[] osds = new OsdFrame[frames.length];
	// for (int k = 0; k < frames.length; k++) {
	// osds[k] = new OsdFrame(frames[k]);
	// }
	// osdMgmt = new OsdFrameManager(osds);
	// osdMgmt.setVmMon(vmMon);
	// } else {
	// LOG.error("Could not re-initilize OSD manager.");
	// }
	// }
	//
	// private JFrame getDefaultParentFrame() {
	// return parentFrames[0];
	// }
	//
	// public JFrame[] getParentFrame() {
	// return parentFrames;
	// }
	//
	// public void setNavigationFrames(NavigationBar[] navbarFrames) {
	// this.navbarFrames = navbarFrames;
	// }
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
