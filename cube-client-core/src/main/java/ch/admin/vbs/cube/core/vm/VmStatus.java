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

/**
 * Represent the status of a {@link Vm}.
 */
public enum VmStatus {
	/**
	 * The status when the product specific status can not be match if another
	 * status or during initial process.
	 */
	UNKNOWN,
	/**
	 * The status when the vm is stopped and ready to be started.
	 */
	STOPPED,
	/**
	 * The status when the vm is starting up, but still not started yet.
	 */
	STARTING,
	/**
	 * The status when the vm is stopping, but still not stopped yet.
	 */
	STOPPING,
	/**
	 * The status when the vm is running and stable to work.
	 */
	RUNNING,
	/**
	 * The status when the vm is not local available and the server, where it is
	 * store, is reachable.
	 */
	STAGABLE,
	/**
	 * The status when the vm is staging from a file or the server.
	 */
	STAGING,
	/**
	 * The status when the vm has an error or something went wrong.
	 */
	ERROR
}
