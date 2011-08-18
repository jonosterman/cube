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

package ch.admin.vbs.cube.core.vm.vbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.core.vm.VmConfig;

public class VBoxConfig extends VmConfig {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(VBoxConfig.class);

	public enum VBoxOption { //
		Disk1File("vbox.disk1.file"), //
		OsType("vbox.ostype"), //
		Acpi("vbox.acpi"), //
		IoApic("vbox.ioApic"), //
		Pae("vbox.pae"), //
		BaseMemory("vbox.baseMemory"), //
		VideoMemory("vbox.videoMemory"), //
		Audio("vbox.audio"), //
		Disk1Size("vbox.disk1Size"), //
		VpnName("vbox.nic1vpn.name"), //
		VpnDescription("vbox.nic1vpn.description"), //
		VpnHostname("vbox.nic1vpn.hostname"), //
		VpnPort("vbox.nic1vpn.port"), //
		VpnClientKey("vbox.nic1vpn.clientKey"), //
		VpnClientCert("vbox.nic1vpn.clientCert"), //
		VpnCaCert("vbox.nic1vpn.caCert"), //
		Nic1("vbox.nic1"), //
		Nic2("vbox.nic2"), //
		Nic3("vbox.nic3"), //
		Nic4("vbox.nic4"), //
		Nic1Mac("vbox.nic1Mac"), //
		Nic2Mac("vbox.nic2Mac"), //
		Nic3Mac("vbox.nic3Mac"), //
		Nic4Mac("vbox.nic4Mac"), //
		Nic1Bridge("vbox.nic1Bridge"), //
		Nic2Bridge("vbox.nic2Bridge"), //
		Nic3Bridge("vbox.nic3Bridge"), //
		Nic4Bridge("vbox.nic4Bridge"), 
		HwUuid("vbox.hwUuid");//
		 //
		private String name;

		VBoxOption(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public VBoxConfig(Container vmContainer, Container rtContainer) {
		super(vmContainer, rtContainer);
	}

	public String getOption(VBoxOption opt) {
		return (String) getProperties().get(opt.getName());
	}

	public long getOptionAsLong(VBoxOption opt) {
		return Long.parseLong((String) getProperties().get(opt.getName()));
	}

	public String getOptionOnOff(VBoxOption opt) {
		String str = (String) getProperties().get(opt.getName());
		return "true".equalsIgnoreCase(str) ? "on" : "off";
	}

	public boolean getOptionAsBoolean(VBoxOption opt) {
		String str = (String) getProperties().get(opt.getName());
		return "true".equalsIgnoreCase(str);
	}

	public void setOption(VBoxOption opt, String value) {
		if (value == null) {
			LOG.warn("Option [{}] should not be null", opt.name);
		} else {
			getProperties().put(opt.getName(), value);
		}
	}
}
