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

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map.Entry;

import ch.admin.vbs.cube.client.wm.client.IVmMonitor;
import ch.admin.vbs.cube.common.CubeClassification;

import com.jidesoft.swing.JideTabbedPane;

/**
 * Provide display colors based on classifications.
 */
public class TabColorProvider implements JideTabbedPane.ColorProvider, JideTabbedPane.GradientColorProvider {
	// UNCLASSIFIED, RESTRICTED, CONFIDENTIAL, SECRET
	private static final HashMap<CubeClassification, Color> COLOR_BASE = new HashMap<CubeClassification, Color>();
	private static final HashMap<CubeClassification, Color> COLOR_SELECTED = new HashMap<CubeClassification, Color>();
	private static final HashMap<CubeClassification, Color> COLOR_SELECGRD = new HashMap<CubeClassification, Color>();
	private static final HashMap<CubeClassification, Color> COLOR_NOTSELECTED = new HashMap<CubeClassification, Color>();
	private static final HashMap<CubeClassification, Color> COLOR_NOTSELECGRD = new HashMap<CubeClassification, Color>();
	static {
		COLOR_BASE.put(CubeClassification.SECRET, Color.RED);
		COLOR_BASE.put(CubeClassification.UNCLASSIFIED, Color.GREEN.darker().darker());
		COLOR_BASE.put(CubeClassification.CONFIDENTIAL, Color.YELLOW.darker().darker());
		COLOR_BASE.put(CubeClassification.RESTRICTED, Color.BLUE);
		// compute selected colors
		for (Entry<CubeClassification, Color> e : COLOR_BASE.entrySet()) {
			COLOR_SELECGRD.put(e.getKey(), colorToWhite(e.getValue(), 1));
			COLOR_SELECTED.put(e.getKey(), colorToWhite(e.getValue(), 1));
		}
		// compute unselected colors
		for (Entry<CubeClassification, Color> e : COLOR_BASE.entrySet()) {
			COLOR_NOTSELECGRD.put(e.getKey(), colorToWhite(e.getValue(), 2).darker().darker());
			COLOR_NOTSELECTED.put(e.getKey(), colorToWhite(e.getValue(), 2).darker().darker().darker().darker());
		}
	}
	private static final Color DEFAULT_BACKGROUND_COLOR = Color.RED;
	private static final Color COLOR_TEXT_SELECTED = new Color(250, 250, 250);
	private static final Color COLOR_TEXT_UNSELECTED = new Color(180, 180, 180);
	// 0.5f means no gradient
	private static final float GRADIENT_RATIO = 0.7f;
	private JideTabbedPane tabbedPane;
	private IVmMonitor vmMon;

	/**
	 * Creates the TabColorProvider which mainly defines the tab background and
	 * foreground color.
	 * 
	 * @param tabbedPane
	 *            the tabbedPane
	 * @param vmMon
	 */
	public TabColorProvider(JideTabbedPane tabbedPane) {
		this.tabbedPane = tabbedPane;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jidesoft.swing.JideTabbedPane.ColorProvider#getBackgroundAt(int)
	 */
	@Override
	public Color getBackgroundAt(int tabIndex) {
		Component component = tabbedPane.getComponentAt(tabIndex);
		if (component != null && component instanceof TabComponent && ((TabComponent) component).getVmHandle() == null) {
			// a special tab with 'null' a VmHandle is displayed if no tab
			// are available. This special tab ensure that the main menu is
			// always
			// accessible.
			return Color.DARK_GRAY;
		}
		CubeClassification classification = CubeClassification.UNCLASSIFIED;
		boolean selected = (tabbedPane.getSelectedIndex() == tabIndex);
		// get tab if possible
		if (component != null && component instanceof TabComponent) {
			TabComponent cmp = (TabComponent) component;
			classification = vmMon.getVmClassification(cmp.getVmHandle());
		}
		return getBackgroundColor(classification, selected);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jidesoft.swing.JideTabbedPane.ColorProvider#getForegroudAt(int)
	 */
	@Override
	public Color getForegroudAt(int tabIndex) {
		Component component = tabbedPane.getComponentAt(tabIndex);
		if (component != null && component instanceof TabComponent && ((TabComponent) component).getVmHandle() == null) {
			// a special tab with 'null' a VmHandle is displayed if no tab
			// are available. This special tab ensure that the main menu is
			// always
			// accessible.
			return Color.DARK_GRAY;
		}
		//
		boolean selected = (tabbedPane.getSelectedIndex() == tabIndex);
		return selected ? COLOR_TEXT_SELECTED : COLOR_TEXT_UNSELECTED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.jidesoft.swing.JideTabbedPane.ColorProvider#getGradientRatio(int)
	 */
	@Override
	public float getGradientRatio(int tabIndex) {
		return GRADIENT_RATIO;
	}

	/**
	 * Get the color for a specific classification.
	 * 
	 * @param classification
	 *            the classification
	 * @param selected
	 *            true for the selected color, otherwise false
	 * 
	 *            {@link Classification}
	 * @return {@link Color}
	 */
	public static Color getBackgroundColor(final CubeClassification classification, final boolean selected) {
		Color color = DEFAULT_BACKGROUND_COLOR;
		if (classification != null) {
			if (selected) {
				color = COLOR_SELECTED.get(classification);
			} else {
				color = COLOR_NOTSELECTED.get(classification);
			}
		}
		return color;
	}

	private static Color colorToWhite(Color c, int iter) {
		float[] cp = c.getColorComponents(null);
		for (int n = 0; n < iter; n++) {
			for (int i = 0; i < cp.length; i++) {
				cp[i] = (1.0f + cp[i]) / 2;
			}
		}
		return new Color(cp[0], cp[1], cp[2]);
	}

	// ###################################################
	// Implements JideTabbedPane.GradientColorProvider
	// ###################################################
	@Override
	public Color getTopBackgroundAt(int tabIndex) {
		CubeClassification classification = null;
		boolean selected = (tabbedPane.getSelectedIndex() == tabIndex);
		// get tab if possible
		Component component = tabbedPane.getComponentAt(tabIndex);
		if (component != null && component instanceof TabComponent) {
			TabComponent cmp = (TabComponent) component;
			if (cmp.getVmHandle() == null) {
				// a special tab with 'null' a VmHandle is displayed if no tab
				// are available. This special tab ensure that the main menu is
				// always
				// accessible.
				return Color.LIGHT_GRAY;
			}
			classification = vmMon.getVmClassification(cmp.getVmHandle());
		}
		Color color = DEFAULT_BACKGROUND_COLOR;
		if (classification != null) {
			if (selected) {
				color = COLOR_SELECGRD.get(classification);
			} else {
				color = COLOR_NOTSELECGRD.get(classification);
			}
		}
		return color;
	}

	public void setVmMon(IVmMonitor vmMon) {
		this.vmMon = vmMon;
	}

	public IVmMonitor getVmMon() {
		return vmMon;
	}
}
