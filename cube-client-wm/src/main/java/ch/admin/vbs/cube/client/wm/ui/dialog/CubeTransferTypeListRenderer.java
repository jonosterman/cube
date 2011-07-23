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

package ch.admin.vbs.cube.client.wm.ui.dialog;

import java.awt.Component;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.common.CubeTransferType;

public class CubeTransferTypeListRenderer extends JLabel implements ListCellRenderer {
	private static final long serialVersionUID = 1L;
	private ResourceBundle resourceBundle = I18nBundleProvider.getBundle();

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (value == null || !(value instanceof CubeTransferType)) {
			setText("" + value);
			return this;
		} else {
			CubeTransferType tt = (CubeTransferType) value;
			setText(resourceBundle.getString("filetransferWizard.label.flavor." + tt.toString()));
		}
		return this;
	}
}
