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

package ch.admin.vbs.cube.core;

import ch.admin.vbs.cube.common.UuidGenerator;

/**
 * Since cube UI is stateless, result of UI request (confirmation dialog, PIN
 * dialog, etc) are not returned in form of a method return value but via a
 * callback object.
 * 
 * This is required in order to be able to lock the screen (user removes its
 * token) at any time, even a confirmation dialog is currently displayed.
 */
public abstract class AbstractUICallback implements IUICallback {
	public final String id;

	public AbstractUICallback() {
		this.id = UuidGenerator.generate();
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return String.format("UICallback [%s]", id);
	}

}
