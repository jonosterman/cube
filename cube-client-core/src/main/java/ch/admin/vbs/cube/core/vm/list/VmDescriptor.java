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
package ch.admin.vbs.cube.core.vm.list;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.admin.vbs.cube.common.CubeClassification;

/**
 * The VmDescriptor contains the VM configuration. One part of the configuration
 * is given by the server and some values are configured locally.
 * 
 * Some local values may 'override' remote one.
 */
public class VmDescriptor {
	private final LocalConfig localCfg = new LocalConfig();
	private final RemoteConfig remoteCfg = new RemoteConfig();

	public VmDescriptor() {
	}

	public LocalConfig getLocalCfg() {
		return localCfg;
	}

	public RemoteConfig getRemoteCfg() {
		return remoteCfg;
	}

	public class LocalConfig {
		private String vmContainerUid;
		private String runtimeContainerUid;
		private Map<String, String> properties = Collections.synchronizedMap(new HashMap<String, String>());

		public void setPropertie(String key, String value) {
			properties.put(key, value);
		}

		public String getPropertie(String key) {
			return properties.get(key);
		}

		public Set<String> getPropertyKeys() {
			return new HashSet<String>(properties.keySet());
		}

		public String getVmContainerUid() {
			return vmContainerUid;
		}

		public void setVmContainerUid(String vmContainerUid) {
			this.vmContainerUid = vmContainerUid;
		}

		public String getRuntimeContainerUid() {
			return runtimeContainerUid;
		}

		public void setRuntimeContainerUid(String runtimeContainerUid) {
			this.runtimeContainerUid = runtimeContainerUid;
		}
	}

	public class RemoteConfig {
		private String description;
		private String name;
		private String id;
		private String domain;
		private String type;
		private CubeClassification classification;
		private String cfgVersion;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public CubeClassification getClassification() {
			return classification;
		}

		public void setClassification(CubeClassification classification) {
			this.classification = classification;
		}

		public String getCfgVersion() {
			return cfgVersion;
		}

		public void setCfgVersion(String cfgVersion) {
			this.cfgVersion = cfgVersion;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
}
