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

import java.util.HashMap;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.ui.dialog.CubeMessageDialog;
import ch.admin.vbs.cube.client.wm.ui.dialog.CubeWizard;
import ch.admin.vbs.cube.core.ISession;
import ch.admin.vbs.cube.core.ISession.ISessionStateDTO;
import ch.admin.vbs.cube.core.ISessionUI;

public class JMockSessionUI implements ISessionUI {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(JMockSessionUI.class);
	private ISession currentSession;
	private HashMap<ISession, CubeWizard> sessionDialog = new HashMap<ISession, CubeWizard>();

	@Override
	public void showDialog(final String message, final ISession session) {
		LOG.debug("show dialog [{}] [{}]", session.getId().getSubjectName(), message);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				CubeWizard dial = null;
				synchronized (sessionDialog) {
					// close current dialog in the given session if present
					dial = sessionDialog.get(session);
					if (dial != null) {
						dial.setVisible(false);
						dial.dispose();
					}
					// create new dialog
					dial = new CubeMessageDialog(null, message, "Message", CubeMessageDialog.TYPE_INFOMATION);
					sessionDialog.put(session, dial);
				}
				// display it if it is the session is visible
				if (session == currentSession) {
					dial.displayWizard();
				}
				// }
			}
		});
	}

	@Override
	public void notifySessionState(ISession session, ISessionStateDTO sessionStateDTO) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void showWorkspace(ISession session) {
		
		
	}
	
	@Override
	public void notifyConnectionState(ConnectionState connectingVpn) {
		// TODO Auto-generated method stub
		
	}
	
	

}
