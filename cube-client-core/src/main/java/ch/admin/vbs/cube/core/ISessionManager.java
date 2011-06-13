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

package ch.admin.vbs.cube.core;

import java.util.List;

/**
 * @see ISession
 */
public interface ISessionManager {
	enum VmCommand {
		START, POWER_OFF, SAVE, STAGE, DELETE
	};

	void start();

	void addListener(ISessionManagerListener l);

	void removeListener(ISessionManagerListener l);

	void closeSession(ISession session);

	List<ISession> getSessions();

	static interface ISessionManagerListener {
		void sessionOpened(ISession session);

		void sessionClosed(ISession session);

		void sessionLocked(ISession session);
	}
}
