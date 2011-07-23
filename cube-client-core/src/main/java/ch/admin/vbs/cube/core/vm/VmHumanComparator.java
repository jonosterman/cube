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

package ch.admin.vbs.cube.core.vm;

import java.util.Comparator;

import ch.admin.vbs.cube.core.vm.list.VmDescriptor.RemoteConfig;

public class VmHumanComparator implements Comparator<Vm> {
	@Override
	public final int compare(Vm arg0, Vm arg1) {
		if (arg0 == null && arg1 == null)
			return 0;
		if (arg0 == null)
			return -1;
		if (arg1 == null)
			return 1;
		if (arg1.getDescriptor() == null && arg1.getDescriptor() == null)
			return 0;
		if (arg0.getDescriptor() == null)
			return -1;
		if (arg1.getDescriptor() == null)
			return 1;
		RemoteConfig r0 = arg0.getDescriptor().getRemoteCfg();
		RemoteConfig r1 = arg0.getDescriptor().getRemoteCfg();
		String s0 = String.format("%s##%s##%s", r0.getClassification().ordinal(), r0.getDomain(), r0.getName()).toUpperCase();
		String s1 = String.format("%s##%s##%s", r1.getClassification().ordinal(), r1.getDomain(), r1.getName()).toUpperCase();
		return s0.compareTo(s1);
	}
}
