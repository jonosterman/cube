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

package ch.admin.vbs.cube.client.wm.mock;

import java.util.ArrayList;
import java.util.List;

import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISessionManager;
import ch.admin.vbs.cube.core.ISessionUI;

public class MockAlreadyOpenedSessionManager implements ISessionManager {
	private ArrayList<ISessionManagerListener> listeners = new ArrayList<ISessionManagerListener>();
	private MockSession session;

	@Override
	public void start() {
		session = new MockSession();
		for (ISessionManagerListener l : listeners) {
			l.sessionOpened(session);
		}
	}

	@Override
	public void addListener(ISessionManagerListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(ISessionManagerListener l) {
	}

	@Override
	public void closeSession(ISession session) {
	}

	@Override
	public List<ISession> getSessions() {
		ArrayList<ISession> list = new ArrayList<ISession>();
		list.add(session);
		return list;
	}

	public void setup(ISessionUI sessionUI) {
	}
}
