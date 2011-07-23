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

import java.io.File;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.core.vm.list.VmDescriptor;

public class Vm implements Cloneable {
	private VmStatus vmStatus;
	private Container vmContainer;
	private Container runtimeContainer;
	private VmDescriptor descriptor;
	private int progress;
	private String progressMessage;
	private File exportFolder;
	private File importFolder;
	private File tempFolder;

	public Vm(VmDescriptor descriptor) {
		this.setDescriptor(descriptor);
		// the current identifier of a vm is the vm name
		this.vmStatus = VmStatus.UNKNOWN;
	}

	public Container getVmContainer() {
		return vmContainer;
	}

	public Container getRuntimeContainer() {
		return runtimeContainer;
	}

	public VmStatus getVmStatus() {
		return vmStatus;
	}

	public void setVmStatus(VmStatus vmStatus) {
		this.vmStatus = vmStatus;
	}

	/**
	 * Returns the progress status in percent depending on the current status.
	 * 
	 * @return the progress in percent
	 */
	public int getProgress() {
		return progress;
	}

	public String getProgressMessage() {
		return progressMessage;
	}

	/**
	 * Sets the progress status in percent depending on the current status.
	 * 
	 * @param progress
	 *            the progress in percent
	 */
	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setProgressMessage(String message) {
		progressMessage = message;
	}

	/**
	 * Returns the unique identifier of the vm.
	 * 
	 * @return the unique identifier
	 */
	public String getId() {
		return getDescriptor().getRemoteCfg().getId();
	}

	public void setVmContainer(Container container) {
		this.vmContainer = container;
	}

	public void setRuntimeContainer(Container container) {
		this.runtimeContainer = container;
	}

	public VmDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && getClass().equals(obj.getClass())) {
			return new EqualsBuilder().append(getId(), ((Vm) obj).getId()).isEquals();
		}
		return false;
	}

	@Override
	public int hashCode() {
		HashCodeBuilder h = new HashCodeBuilder().append(getId());
		return h.hashCode();
	}

	public Vm clone() {
		try {
			Vm clone = (Vm) super.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Failed to clone VM.", e);
		}
	}

	public void setExportFolder(File exportFolder) {
		this.exportFolder = exportFolder;
	}

	public File getExportFolder() {
		return exportFolder;
	}

	public void setImportFolder(File importFolder) {
		this.importFolder = importFolder;
	}

	public File getImportFolder() {
		return importFolder;
	}

	public void setTempFolder(File tempFolder) {
		this.tempFolder = tempFolder;
	}

	public File getTempFolder() {
		return tempFolder;
	}

	public void setDescriptor(VmDescriptor descriptor) {
		this.descriptor = descriptor;
	}
}
