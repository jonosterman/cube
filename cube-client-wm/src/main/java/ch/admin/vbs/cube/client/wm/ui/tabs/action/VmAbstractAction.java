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

package ch.admin.vbs.cube.client.wm.ui.tabs.action;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import ch.admin.vbs.cube.client.wm.client.VmHandle;

/**
 * This class provides default implementations (text and uuid) for virtual
 * machine action. The developer needs only subclass this abstract class and
 * define the exec method.
 */
public abstract class VmAbstractAction extends AbstractAction {
	private static final long serialVersionUID = 0L;
	/**
	 * All registered VmActionListeners.
	 */
	private static List<IVmActionListener> vmActionListeners = new LinkedList<IVmActionListener>();
	private VmHandle vmHandle;

	/**
	 * Creates an abstract action with the text and the vmId.
	 * 
	 * @param text
	 *            text of the action
	 * @param vmId
	 *            vmId of the virtual machine
	 */
	public VmAbstractAction(final String text, final Icon icon, final VmHandle vmHandle) {
		super(text, icon);
		if (vmHandle == null) {
			throw new NullPointerException("vmHandle should not be null");
		}
		this.vmHandle = vmHandle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public final void actionPerformed(ActionEvent actionEvent) {
		exec(actionEvent);
	}

	/**
	 * Returns the vmId of the virtual machine to which the action belongs to.
	 * 
	 * @return vmId vmId of the virual machine
	 */
	public VmHandle getVmHandle() {
		return vmHandle;
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 * @param actionEvent
	 *            ActionEvent which was invoke
	 */
	public abstract void exec(ActionEvent actionEvent);

	/**
	 * Registers <code>VmActionListener</code> so that it will receive virtual
	 * machine action events.
	 * 
	 * @param l
	 *            VmActionListener to register
	 */
	public static synchronized void addVmActionListener(IVmActionListener l) {
		vmActionListeners.add(l);
	}

	/**
	 * Unregisters <code>VmActionListener</code> so that it will no longer
	 * receive virtual machine action events.
	 * 
	 * @param l
	 *            vmActionListener to be removed
	 */
	public static synchronized void removeActionListener(IVmActionListener l) {
		vmActionListeners.remove(l);
	}

	/**
	 * Returns a copy of all registered <code>VmActionListener</code>s.
	 * 
	 * @return all registered VmActionListeners
	 */
	protected synchronized IVmActionListener[] getVmActionListeners() {
		return vmActionListeners.toArray(new IVmActionListener[] {});
	}
}
