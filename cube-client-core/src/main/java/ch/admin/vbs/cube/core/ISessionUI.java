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

import ch.admin.vbs.cube.core.ISession.ISessionStateDTO;

/**
 * Define a simplistic session UI where only one dialog could be displayed at
 * time. If no dialog is opened, then the VM list is displayed at the top of the
 * UI (tabs).
 */
public interface ISessionUI {
	void showDialog(String message, ISession session);

	void showWorkspace(ISession session);

	void notifySessionState(ISession session, ISessionStateDTO sessionStateDTO);
	

	
}
