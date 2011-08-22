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

package ch.admin.vbs.cube.client.wm.ui.tabs;

import java.awt.Dimension;

import javax.swing.plaf.UIResource;

import ch.admin.vbs.cube.client.wm.utils.IconManager;

import com.jidesoft.swing.JideLabel;
import com.jidesoft.swing.JideTabbedPane;

/**
 * LogoPanel is a JideLabel that holds the swiss icon on it without any text.
 * The important part of this class is the implementation of the marker
 * interface UIResource, so that it is possible to use this component as the
 * leading component of the class {@link JideTabbedPane}.
 * 
 * 
 * @see JideTabbedPane#setTabLeadingComponent(java.awt.Component)
 */
public class LogoPanel extends JideLabel implements UIResource {
	private static final long serialVersionUID = 1L;
	private static final String SWISS_ICON_IMAGE_NAME = "offline_small.png";

	/**
	 * Creates the Panel with the swiss icon on it.
	 */
	public LogoPanel() {
		super(IconManager.getInstance().getIcon(SWISS_ICON_IMAGE_NAME));
		setPreferredSize(new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight()));
	}
}
