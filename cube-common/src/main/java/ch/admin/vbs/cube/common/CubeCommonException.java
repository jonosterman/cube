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

package ch.admin.vbs.cube.common;

/**
 * This exception is thrown when something goes wrong.
 */
public class CubeCommonException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Refer to the documentation of {@link Exception}.
	 */
	public CubeCommonException() {
		super();
	}

	/**
	 * Refer to the documentation of {@link Exception}.
	 * 
	 * @param message
	 *            the detail message.
	 * @param cause
	 *            the cause of this {@link CubeCommonException}.
	 */
	public CubeCommonException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Refer to the documentation of {@link Exception}.
	 * 
	 * @param message
	 *            the detail message.
	 */
	public CubeCommonException(String message) {
		super(message);
	}

	/**
	 * Refer to the documentation of {@link Exception}.
	 * 
	 * @param cause
	 *            the cause of this {@link CubeCommonException}.
	 */
	public CubeCommonException(Throwable cause) {
		super(cause);
	}
}
