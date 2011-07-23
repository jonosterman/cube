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

package ch.admin.vbs.cube.core.network.vpn;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.keyring.IKeyring;
import ch.admin.vbs.cube.common.keyring.SafeFile;
import ch.admin.vbs.cube.common.shell.ScriptUtil;
import ch.admin.vbs.cube.core.network.vpn.VpnConfig.VpnOption;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmException;
import ch.admin.vbs.cube.core.webservice.InstanceParameterHelper;
import cube.cubemanager.services.InstanceConfigurationDTO;

public class VpnManager {
	private static final Logger LOG = LoggerFactory.getLogger(VpnManager.class);
	private ExecutorService exs = Executors.newCachedThreadPool();

	/**
	 * During stagging, th vpnmanager will just sotre its config in runtime
	 * container.
	 * 
	 * @param vm
	 * @param remoteCfg
	 * @param keyring
	 */
	public void stagging(Vm vm, InstanceConfigurationDTO remoteCfg, IKeyring keyring) throws VmException {
		try {
			VpnConfig cfg = new VpnConfig(vm.getVmContainer(), vm.getRuntimeContainer());
			cfg.load();
			if (InstanceParameterHelper.getInstanceParameterAsBoolean("vpn.enabled", remoteCfg)) {
				// update 'cfg' with values from 'remoteCfg'
				cfg.setOption(VpnOption.Tap, InstanceParameterHelper.getInstanceParameter("vpn.tap", remoteCfg));
				cfg.setOption(VpnOption.Name, InstanceParameterHelper.getInstanceParameter("vpn.name", remoteCfg));
				cfg.setOption(VpnOption.Description, InstanceParameterHelper.getInstanceParameter("vpn.description", remoteCfg));
				cfg.setOption(VpnOption.Hostname, InstanceParameterHelper.getInstanceParameter("vpn.hostname", remoteCfg));
				cfg.setOption(VpnOption.Port, InstanceParameterHelper.getInstanceParameter("vpn.port", remoteCfg));
				keyring.storeData(InstanceParameterHelper.getInstanceParameter("vpn.clientKey", remoteCfg).getBytes("UTF-8"), vm.getId() + "."
						+ VpnOption.ClientKey.getName());
				keyring.storeData(InstanceParameterHelper.getInstanceParameter("vpn.clientCert", remoteCfg).getBytes("UTF-8"), vm.getId() + "."
						+ VpnOption.ClientCert.getName());
				keyring.storeData(InstanceParameterHelper.getInstanceParameter("vpn.caCert", remoteCfg).getBytes("UTF-8"),
						vm.getId() + "." + VpnOption.CaCert.getName());
				cfg.setOption(VpnOption.Enabled, "true");
			} else {
				// reset all vpn config values
				for (VpnOption o : VpnOption.values()) {
					cfg.setOption(o, null);
				}
				cfg.setOption(VpnOption.Enabled, "false");
			}
			cfg.save();
		} catch (Exception e) {
			throw new VmException("Failed to stage VirtualBox VM.", e);
		}
	}

	public void openVpn(final Vm vm, final IKeyring keyring) throws VmException {
		exs.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// load configuration
					VpnConfig cfg = new VpnConfig(vm.getVmContainer(), vm.getRuntimeContainer());
					cfg.load();
					// check if vpn is enabled
					if (!cfg.getOptionAsBoolean(VpnOption.Enabled)) {
						return;
					}
					// retrieve keys from keyring
					SafeFile tmpKey = keyring.retrieveDataAsFile(vm.getId() + "." + VpnOption.ClientKey.getName());
					SafeFile tmpCert = keyring.retrieveDataAsFile(vm.getId() + "." + VpnOption.ClientCert.getName());
					SafeFile tmpCa = keyring.retrieveDataAsFile(vm.getId() + "." + VpnOption.CaCert.getName());
					if (!tmpKey.exists() || !tmpCert.exists() || !tmpCa.exists())
						throw new VmException("Cert/keys have not been decrypted correctly");
					// open VPN tunnel
					ScriptUtil script = new ScriptUtil();
					script.execute("sudo", "./vpn-open.pl", //
							"--tap", cfg.getOption(VpnOption.Tap),//
							"--hostname", cfg.getOption(VpnOption.Hostname),//
							"--port", cfg.getOption(VpnOption.Port),//
							"--ca", tmpCa.getAbsolutePath(),//
							"--cert", tmpCert.getAbsolutePath(),//
							"--key", tmpKey.getAbsolutePath()//
					);
					// shred keys since they are no more needed
					tmpKey.shred();
					tmpCa.shred();
					tmpCert.shred();
				} catch (Exception e) {
					LOG.error("Failed to start VPN connection", e);
				}
			}
		});
	}

	public void closeVpn(Vm vm) throws VmException {
		try {
			// load configuration
			VpnConfig cfg = new VpnConfig(vm.getVmContainer(), vm.getRuntimeContainer());
			cfg.load();
			// check if vpn is enabled
			if (!cfg.getOptionAsBoolean(VpnOption.Enabled)) {
				return;
			}
			// open VPN tunnel
			ScriptUtil script = new ScriptUtil();
			script.execute("sudo", "./vpn-close.pl", //
					"--tap", cfg.getOption(VpnOption.Tap)//
			);
		} catch (Exception e) {
			throw new VmException("Failed to close VPN.", e);
		}
	}
}
