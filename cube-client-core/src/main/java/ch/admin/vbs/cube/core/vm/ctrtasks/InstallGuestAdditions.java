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

package ch.admin.vbs.cube.core.vm.ctrtasks;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;
import ch.admin.vbs.cube.core.vm.Vm;

public class InstallGuestAdditions implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(InstallGuestAdditions.class);
	private final Vm vm;

	public InstallGuestAdditions(Vm vm) {
		this.vm = vm;
	}

	@Override
	public void run() {
		LOG.debug("Attach VBox installation CD to VM [{}]", vm.getId());
		ShellUtil su = new ShellUtil();
		ArrayList<String> commandWithArgs = new ArrayList<String>();
		commandWithArgs.add("VBoxManage");
		commandWithArgs.add("storageattach");
		commandWithArgs.add(vm.getId());
		commandWithArgs.add("--storagectl");
		commandWithArgs.add("IDE Controller");
		commandWithArgs.add("--device");
		commandWithArgs.add("0");
		commandWithArgs.add("--port");
		commandWithArgs.add("1");
		commandWithArgs.add("--type");
		commandWithArgs.add("dvddrive");
		commandWithArgs.add("--medium");
		commandWithArgs.add("/usr/share/virtualbox/VBoxGuestAdditions.iso");
		try {
			su.run(commandWithArgs);
		} catch (ShellUtilException e) {
			LOG.error("Failed to mount VBoxGuestAddtions CD-ROM", e);
		}
	}
}
