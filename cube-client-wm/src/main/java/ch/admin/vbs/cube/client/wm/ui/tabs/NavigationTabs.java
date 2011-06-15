/**
 * Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package ch.admin.vbs.cube.client.wm.ui.tabs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.IVmControl;
import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.CubeLogoutAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.CubeShutdownAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmAttachUsbDevice;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmDeleteAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmDetachUsbDevice;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmInstallAdditionsAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmPoweroffAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmSaveAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmStageAction;
import ch.admin.vbs.cube.client.wm.ui.tabs.action.VmStartAction;
import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.client.wm.utils.IconManager;
import ch.admin.vbs.cube.core.ICoreFacade;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntry;
import ch.admin.vbs.cube.core.usb.UsbDeviceEntryList;
import ch.admin.vbs.cube.core.vm.VmStatus;

import com.jidesoft.swing.JideMenu;
import com.jidesoft.swing.JidePopupMenu;
import com.jidesoft.swing.JideTabbedPane;

public class NavigationTabs extends JideTabbedPane {
	private static final long serialVersionUID = 1L;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(NavigationTabs.class);
	private IVmControl vmCtrl;
	private IVmMonitor vmMon;
	private TabColorProvider colorProvider;
	private JidePopupMenu cubePopupMenu;
	private JPopupMenu vmPopupMenu;
	private final int monitorCount;
	private final int monitorIdx;
	private ICoreFacade core;

	public NavigationTabs(int monitorCount, int monitorIdx) {
		this.monitorCount = monitorCount;
		this.monitorIdx = monitorIdx;
		// default settings
		setTabResizeMode(RESIZE_MODE_FIXED); // exact size set in CubeUIDefaults
		setShowGripper(false); //
		setShowTabButtons(true);
		setTabShape(SHAPE_DEFAULT);
		setColorTheme(COLOR_THEME_DEFAULT);
		setRightClickSelect(true);
		colorProvider = new TabColorProvider(this);
		setTabColorProvider(colorProvider);
		// setShowGripper(false);
		setShowTabArea(true);
		setShowTabContent(false);
		// we are interested in mouse clicks
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				int tabIndex = getTabAtLocation(mouseEvent.getX(), mouseEvent.getY());
				if (tabIndex >= 0) {
					TabComponent tabIdentifier = (TabComponent) getComponentAt(tabIndex);
					if (mouseEvent.isPopupTrigger()) {
						showTabPopupMenu(NavigationTabs.this, mouseEvent, tabIdentifier.getVmHandle(), tabIndex);
					}
				}
			}
		});
		// we are interested in select changes for example: first tab, tab
		// change per keyboard, ...
		addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (isVisible()) {
					LOG.trace("stateChanged");
					synchronized (NavigationTabs.this) {
						if (NavigationTabs.this.equals(e.getSource()) && vmCtrl != null) {
							int tabIndex = getSelectedIndex();
							if (tabIndex >= 0) {
								Object o = getComponentAt(tabIndex);
								if (o instanceof TabComponent) {
									TabComponent comp = (TabComponent) getComponentAt(tabIndex);
									if (comp != null) {
										vmCtrl.showVm(comp.getVmHandle());
									} else {
										vmCtrl.hideAllVms(NavigationTabs.this.monitorIdx);
									}
								}
							} else {
								vmCtrl.hideAllVms(NavigationTabs.this.monitorIdx);
							}
						}
					}
				}
			}
		});
		// add header logo
		if (monitorIdx == 0) {
			LogoPanel logo = new LogoPanel();
			logo.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					showLogoPopupMenu(e);
				}
			});
			setTabLeadingComponent(logo);
		}
	}

	/**
	 * Paint a custom line in order to hide unnecessary gray border between the
	 * tab and tab content. The tab, the content border and the line we draw in
	 * between match the classification color.
	 */
	@Override
	protected void paintChildren(Graphics g) {
		super.paintChildren(g);
		// get color of selected tab
		int i = getSelectedIndex();
		if (i >= 0) {
			Color c = colorProvider.getBackgroundAt(i);
			if (c != null) {
				g.setColor(c);
				g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
			}
		}
	}

	/**
	 * Shows the popup menu for the given tab.
	 * 
	 * @param parent
	 *            parent which the popup menu belongs to
	 * @param event
	 *            mouse event with the coordinates
	 * @param h
	 *            vmId of the virtual machine
	 * @param tabIndex
	 *            the index of the tab
	 */
	private void showTabPopupMenu(final Component parent, final MouseEvent event, final VmHandle h, final int tabIndex) {
		LOG.debug("showTabPopupMenu [{}]", h);
		if (h == null)
			return;
		ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
		VmStatus state = vmMon.getVmState(h);
		// prepare popup menu
		// JidePopupMenu popupMenu = new JidePopupMenu();
		// @03032010: JidePopupMenu is to slow (openjdk6) replace with
		// JPopupMenu
		vmPopupMenu = new JPopupMenu();
		switch (state) {
		case STOPPED:
			vmPopupMenu.add(new VmStartAction(h));
			vmPopupMenu.add(new VmDeleteAction(h));
			break;
		case RUNNING:
			vmPopupMenu.add(new VmSaveAction(h));
			// popupMenu.add(new VmStopAction(vmId));
			vmPopupMenu.add(new VmPoweroffAction(h));
			vmPopupMenu.addSeparator();
			vmPopupMenu.add(new VmInstallAdditionsAction(h));
			break;
		case STAGABLE:
			vmPopupMenu.add(new VmStageAction(h));
			break;
		case STAGING:
			vmPopupMenu.add(new VmStageAction(h, false));
			break;
		case STARTING:
		case STOPPING:
			vmPopupMenu.add(new VmSaveAction(h, false));
			// popupMenu.add(new VmStopAction(vmId, false));
			vmPopupMenu.add(new VmPoweroffAction(h, false));
			break;
		// internal errors
		case ERROR:
		case UNKNOWN:
			vmPopupMenu.add(new VmPoweroffAction(h));
			vmPopupMenu.add(new VmDeleteAction(h));
			break;
		default:
			LOG.error("No menu for state [" + state + "]");
			return;
		}
		// add USB menu
		if (state == VmStatus.RUNNING) {
			vmPopupMenu.addSeparator();
			// vmPopupMenu.add(new VmConnectUsbDevice(h));
			//
			UsbDeviceEntryList list = core.getUsbDeviceList(h.getVmId());
			JideMenu usbMenu = new JideMenu(I18nBundleProvider.getBundle().getString("cube.action.connectusb.text"));
			usbMenu.setIcon(IconManager.getInstance().getIcon("usb_icon16.png"));
			vmPopupMenu.add(usbMenu);
			if (list.size() == 0) {
				usbMenu.setEnabled(false);
			} else {
				usbMenu.setEnabled(true);
				// add usb device as checkbox items
				for (UsbDeviceEntry e : list) {
					switch (e.getState()) {
					case ALREADY_ATTACHED: {
						JCheckBoxMenuItem chk = new JCheckBoxMenuItem(new VmDetachUsbDevice(h, e));
						chk.setSelected(true);
						usbMenu.add(chk);
						break;
					}
					case AVAILABLE: {
						JCheckBoxMenuItem chk = new JCheckBoxMenuItem(new VmAttachUsbDevice(h, e));
						chk.setSelected(false);
						usbMenu.add(chk);
						break;
					}
					default:
						LOG.error("State not supported [{}]", e.getState());
					case ATTACHED_TO_ANOTHER_VM: {
						JMenuItem not = new JMenuItem(e.getDevice().toString());
						not.setEnabled(false);
						usbMenu.add(not);
						break;
					}
					}
				}
			}
		}
		// add Monitor move menu
		if (monitorCount > 1) {
			vmPopupMenu.addSeparator();
			JideMenu monitorMenu = new JideMenu(resourceBundle.getString("vm.move.monitor.menu"));
			vmPopupMenu.add(monitorMenu);
			for (int i = 0; i < monitorCount; ++i) {
				if (i != monitorIdx) {
					final int destMonitorIndex = i;
					// Monitor number starts with 1
					JMenuItem item = new JMenuItem(MessageFormat.format(resourceBundle.getString("vm.move.monitor.menu.item"), i + 1));
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							vmCtrl.moveVm(h, destMonitorIndex);
						}
					});
					monitorMenu.add(item);
				}
			}
		}
		// show popup
		LOG.debug("showTabPopupMenu .. show");
		vmPopupMenu.show(parent, event.getX(), event.getY());
	}

	private void showLogoPopupMenu(final MouseEvent mouseEvent) {
		// fill popup menu with actions
		cubePopupMenu = new JidePopupMenu();
		// cubePopupMenu.add(new CubeLockAction());
		cubePopupMenu.add(new CubeLogoutAction());
		cubePopupMenu.addSeparator();
		cubePopupMenu.add(new CubeShutdownAction());
		// place menu under the logo button
		JComponent comp = (JComponent) mouseEvent.getSource();
		cubePopupMenu.show(comp, comp.getX(), comp.getY() + comp.getHeight());
	}

	public void setVmCtrl(IVmControl vmCtrl) {
		this.vmCtrl = vmCtrl;
	}

	public IVmControl getVmCtrl() {
		return vmCtrl;
	}

	public void setVmMon(IVmMonitor vmMon) {
		this.vmMon = vmMon;
		colorProvider.setVmMon(vmMon);
	}

	public IVmMonitor getVmMon() {
		return vmMon;
	}

	public void setCore(ICoreFacade core) {
		this.core = core;
	}
}
