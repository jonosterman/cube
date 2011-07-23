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

package ch.admin.vbs.cube.client.wm.dialog;

import javax.swing.SwingUtilities;

import org.junit.Ignore;
import org.junit.Test;

import ch.admin.vbs.cube.client.wm.client.ICubeActionListener;
import ch.admin.vbs.cube.client.wm.ui.dialog.CubeInitialDialog;
import ch.admin.vbs.cube.client.wm.utils.CubeUIDefaults;
import ch.admin.vbs.cube.core.usb.UsbDevice;

public class CubeInitialDialogDemo {
	public static void main(String[] args) {
		CubeInitialDialogDemo demo = new CubeInitialDialogDemo();
		demo.testDialog();
	}

	@Test
	@Ignore
	public void testDialog() {
		System.out.println("dial1");
		CubeUIDefaults.initDefaults();
		final CubeInitialDialog msgdialog = new CubeInitialDialog(null, "Please insert your smart card",new ICubeActionListener() {
			@Override
			public void shutdownMachine() {
			}
			
			@Override
			public void logoutUser() {
			}
			
			@Override
			public void lockCube() {
			}
			
			
			@Override
			public void enteredConfirmation(int result, String requestId) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void enteredPassword(char[] password, String requestId) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void enteredUsbDevice(UsbDevice object, String requestId) {
				// TODO Auto-generated method stub
				
			}
		});
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				msgdialog.displayWizard();
			}
		});

		
		System.out.println("done.");
	}
}
