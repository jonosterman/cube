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

package ch.admin.vbs.cube.core.network.vpn;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.core.vm.VmConfig;

public class VpnConfig extends VmConfig {
	public enum VpnOption { //
		Enabled("vpn.enabled"), //
		Tap("vpn.tap"), //
		Name("vpn.name"), //
		Description("vpn.description"), //
		Hostname("vpn.hostname"), //
		Port("vpn.port"), //
		ClientKey("vpn.clientKey"), //
		ClientCert("vpn.clientCert"), //
		CaCert("vpn.caCert"); //
		private String name;

		VpnOption(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public VpnConfig(Container vmContainer, Container rtContainer) {
		super(vmContainer, rtContainer);
	}

	public String getOption(VpnOption opt) {
		return (String) getProperties().get(opt.getName());
	}

	public boolean getOptionAsBoolean(VpnOption opt) {
		String str = (String) getProperties().get(opt.getName());
		return "true".equalsIgnoreCase(str);
	}

	public void setOption(VpnOption opt, String value) {
		if (value == null) {
			getProperties().remove(opt.getName());
		} else {
			getProperties().put(opt.getName(), value);
		}
	}
}
