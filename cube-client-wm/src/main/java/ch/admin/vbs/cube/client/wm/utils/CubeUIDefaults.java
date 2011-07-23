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

package ch.admin.vbs.cube.client.wm.utils;

import java.awt.Color;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * Some swing/jide setting have to be initialized in UIDefaults in a very early
 * stage. This class should be used for that purpose.
 * 
 */
public class CubeUIDefaults {
	public static final void initDefaults() {
		UIDefaults uiDefaults = UIManager.getDefaults();
		//
		uiDefaults.put("JideTabbedPane.fixedStyleRectSize", 170);
		// uiDefaults.put("JideTabbedPane.border",15);// not working
		uiDefaults.put("JideTabbedPane.background", Color.WHITE);// tab borders
		uiDefaults.put("JideTabbedPane.foreground", new Color(0, 0, 0));// Text
																		// color
		uiDefaults.put("JideTabbedPane.light", new Color(255, 255, 255));// Color
																			// One
																			// of
																			// the
																			// colors
																			// used
																			// to
																			// paint
																			// the
																			// tab
																			// border
		uiDefaults.put("JideTabbedPane.highlight", new Color(100, 100, 100));// not
																				// working
		uiDefaults.put("JideTabbedPane.shadow", Color.LIGHT_GRAY);// tab borders
																	// + basline
																	// borders
		uiDefaults.put("JideTabbedPane.darkShadow", Color.BLUE);// not working
		// uiDefaults.put("JideTabbedPane.tabInsets",new Insets(3,2,2,2));//
		// Insets The insets of each tab
		// uiDefaults.put("JideTabbedPane.contentBorderInsets",new
		// Insets(10,40,30,20));// Insets The insets of tab content
		// uiDefaults.put("JideTabbedPane.tabAreaInsets",new Insets(5,2,2,2));//
		// Insets The insets of the area where all the tabs are
		uiDefaults.put("JideTabbedPane.tabAreaBackground", Color.RED);// Default
																		// tab's
																		// background
																		// color
		// uiDefaults.put("JideTabbedPane.font",);// Font The font used by
		// tabbed pane
		// uiDefaults.put("JideTabbedPane.selectedTabFont",);// Font The font
		// used to draw the text of the selected tab
		uiDefaults.put("JideTabbedPane.unselectedTabTextForeground", Color.CYAN);// Color
																					// The
																					// default
																					// text
																					// color
																					// of
																					// unselected
																					// tabs.
		// uiDefaults.put("JideTabbedPane.textIconGap",);// Integer The gap
		// between icon and text
		uiDefaults.put("JideTabbedPane.showIconOnTab", true);// Boolean Whether
																// to show icon
																// on tabs
		uiDefaults.put("JideTabbedPane.showCloseButtonOnTab", true);// Boolean
																	// Whether
																	// to show
																	// close
																	// button on
																	// tabs
		// uiDefaults.put("JideTabbedPane.closeButtonAlignment",);//
	}
}
