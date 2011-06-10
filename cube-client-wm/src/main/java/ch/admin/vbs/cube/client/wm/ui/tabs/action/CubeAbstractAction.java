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

import ch.admin.vbs.cube.client.wm.client.ICubeActionListener;

/**
 * This class provides default implementations (text) for cube action. The
 * developer needs only subclass this abstract class and define the exec method.
 * 
 * 
 */
public abstract class CubeAbstractAction extends AbstractAction {
	private static final long serialVersionUID = -3580816384103507934L;
	/**
	 * All registered CubeActionListeners.
	 */
	private static List<ICubeActionListener> cubeActionListeners = new LinkedList<ICubeActionListener>();

	/**
	 * Creates an abstract action with the text.
	 * 
	 * @param text
	 *            text of the action
	 */
	public CubeAbstractAction(final String text, final Icon icon) {
		super(text, icon);
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
	 * Invoked when an action occurs.
	 * 
	 * @param actionEvent
	 *            ActionEvent which was invoke
	 */
	public abstract void exec(ActionEvent actionEvent);

	/**
	 * Registers <code>CubeActionListener</code> so that it will receive cube
	 * action events.
	 * 
	 * @param cubeActionListener
	 *            CubeActionListener to register
	 */
	public static synchronized void addCubeActionListener(ICubeActionListener cubeActionListener) {
		cubeActionListeners.add(cubeActionListener);
	}

	/**
	 * Unregisters <code>CubeActionListener</code> so that it will no longer
	 * receive cube action events.
	 * 
	 * @param cubeActionListener
	 *            cubeActionListener to be removed
	 */
	public static synchronized void removeCubeActionListener(ICubeActionListener cubeActionListener) {
		cubeActionListeners.remove(cubeActionListener);
	}

	/**
	 * Returns a copy of all registered <code>CubeActionListener</code>s.
	 * 
	 * @return all registered CubeActionListeners
	 */
	protected synchronized ICubeActionListener[] getCubeActionListeners() {
		return cubeActionListeners.toArray(new ICubeActionListener[] {});
	}
}
