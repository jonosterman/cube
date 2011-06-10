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

package ch.admin.vbs.cube.common.container;

import java.io.File;

/**
 * A Container holds files (configurations, keys, disk images, etc) on the disk.
 * It could be mounted to access data contained in it and then unmounted. The
 * idea is to have encrypted containers, but the implementation how/if it is
 * encrypted is delegate to ContainerFactory implementations.
 */
public class Container {
	/** a unique ID (used to generated file and mount-point names) */
	private String id;
	/** Container file */
	private File containerFile;
	/** Mount point for this container */
	private File mountpoint;
	/** Container size in bytes (if applicable) */
	private long size;

	public Container() {
	}

	public File getContainerFile() {
		return containerFile;
	}

	public void setContainerFile(File containerFile) {
		this.containerFile = containerFile;
	}

	public File getMountpoint() {
		return mountpoint;
	}

	public void setMountpoint(File mountpoint) {
		this.mountpoint = mountpoint;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public boolean exists() {
		return containerFile != null && containerFile.exists();
	}
}
