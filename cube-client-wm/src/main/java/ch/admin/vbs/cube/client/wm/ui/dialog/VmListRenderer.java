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
import java.awt.Dimension;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import ch.admin.vbs.cube.core.vm.Vm;

public class VmListRenderer extends DefaultListCellRenderer {
	private static final long serialVersionUID = 1L;

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (value == null || !(value instanceof Vm)) {
			return super.getListCellRendererComponent(list, String.valueOf(value), index, isSelected, cellHasFocus);
		} else {
			Vm vm = (Vm) value;
			if (vm.getDescriptor() == null) {
				value = "Vm {" + vm.getId() + "}";
			} else {
				value = vm.getDescriptor().getRemoteCfg().getName() + " (" + vm.getDescriptor().getRemoteCfg().getDomain() + " / "
						+ vm.getDescriptor().getRemoteCfg().getClassification() + ")";
			}
		}
		Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		cmp.setPreferredSize(new Dimension(120, 15));
		return cmp;
	}
}
