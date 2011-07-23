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

package ch.admin.vbs.cube.client.wm.client;

import java.util.Comparator;

public class VmHandleHumanComparator implements Comparator<VmHandle> {
	private final IVmMonitor m;

	public VmHandleHumanComparator(IVmMonitor monitor) {
		this.m = monitor;
	}

	@Override
	public int compare(VmHandle arg0, VmHandle arg1) {
		if (arg0 == null && arg1 == null)
			return 0;
		if (arg0 == null)
			return -1;
		if (arg1 == null)
			return 1;
		String s0 = String.format("%s##%s##%s", m.getVmClassification(arg0).ordinal(), m.getVmDomain(arg0), m.getVmName(arg0).toUpperCase());
		String s1 = String.format("%s##%s##%s", m.getVmClassification(arg1).ordinal(), m.getVmDomain(arg1), m.getVmName(arg1).toUpperCase());
		return s0.compareTo(s1);
	}
}
