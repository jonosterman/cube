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

package ch.admin.vbs.cube.client.wm.client.impl;

import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.client.ICubeClient;
import ch.admin.vbs.cube.client.wm.client.IUserInterface;
import ch.admin.vbs.cube.client.wm.client.IUserInterface.ConnectionIcon;
import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.IClientFacade;
import ch.admin.vbs.cube.core.ISession.ISessionStateDTO;
import ch.admin.vbs.cube.core.network.INetManager;
import ch.admin.vbs.cube.core.vm.Vm;

/**
 * This class implements IClientFacade and therefore should handle core
 * requests.
 * 
 */
public class ClientFacade implements IClientFacade {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ClientFacade.class);
	private IUserInterface userIface;
	private ICubeClient client;
	private HashSet<String> cachedVmIds = new HashSet<String>();

	@Override
	public void askConfirmation(String messageKey, String requestId) {
		userIface.showConfirmationDialog(messageKey, requestId);
	}

	
	
	@Override
	public void displayTabs(List<Vm> vmList) {
		// update VM list (it will trigger a VmChangeEvent to update
		// NavigationBar)
		synchronized (cachedVmIds) {
			client.setVms(vmList);
			// cache
			cachedVmIds.clear();
			for (Vm vm : vmList) {
				cachedVmIds.add(vm.getId());
			}
		}
		// hide all dialogs and display VMs and navigation bars
		userIface.showVms();
	}

	@Override
	public void notifiyVmUpdated(Vm vm) {
		synchronized (cachedVmIds) {
			if (cachedVmIds.contains(vm.getId())) {
				client.updateVm(vm);
			}
		}
	}

	@Override
	public void showGetPIN(String additionalMessage, String requestId) {
		userIface.showPinDialog(additionalMessage, requestId);
	}

	@Override
	public void showMessage(String message, int options) {
		userIface.showMessageDialog(message, options);
	}

	@Override
	public void showTransferWizard(Vm vm, RelativeFile file) {
		VmHandle h = client.getVmHandle(vm);
		if (h == null) {
			LOG.error("VM [{}] is not known by CubeClient class. 'showTransferWizard' request for file [{}] will be ignored.", vm, file);
		} else {
			userIface.showTransferDialog(client.getVmHandle(vm), file);
		}
	}

	@Override
	public void notifySessionStateUpdate(ISessionStateDTO state) {
		// userIface.setSessionStateIcon(state.isOnline());
		// TODO remove??? or combine with connection icon??
	}

	@Override
	public void notifyConnectionStateUpdate(INetManager.NetState state) {
		switch (state) {
		case CONNECTED_DIRECT:
			userIface.setSessionStateIcon(ConnectionIcon.CONNECTED);
			break;
		case CONNECTED_BY_VPN:
			userIface.setSessionStateIcon(ConnectionIcon.CONNECTED_VPN);
			break;
		case DEACTIVATED:
			userIface.setSessionStateIcon(ConnectionIcon.NOT_CONNECTED);
			break;
		case CONNECTING_VPN:
			userIface.setSessionStateIcon(ConnectionIcon.CONNECTING_VPN);
			break;
		case CONNECTING:
			userIface.setSessionStateIcon(ConnectionIcon.CONNECTING);
			break;
		}
	}

	// #######################################################
	// Injections
	// #######################################################
	public void setup(ICubeClient client, IUserInterface userIface) {
		this.userIface = userIface;
		this.client = client;
	}
}
