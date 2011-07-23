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

import org.junit.Ignore;
import org.junit.Test;

import ch.admin.vbs.cube.client.wm.ui.dialog.CubeConfirmationDialog;
import ch.admin.vbs.cube.client.wm.ui.dialog.CubeMessageDialog;

public class CubeDialogDemo {
	public static void main(String[] args) {
		CubeDialogDemo demo = new CubeDialogDemo();
		demo.testDialog();
	}

	@Test
	@Ignore
	public void testDialog() {
		CubeConfirmationDialog dial0 = new CubeConfirmationDialog(null, "messagedialog.confirmation.deleteVmConfirmation",
				CubeConfirmationDialog.TYPE_CANCEL_YES);		
		dial0.displayWizard();
		
		
		
		System.out.println("dial1");
		CubeMessageDialog dial1 = new CubeMessageDialog(null, "Test INFO: [àägqkpXXW] Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore ", "Test: [àägqkpXXW]",
				CubeMessageDialog.TYPE_INFOMATION);
		dial1.displayWizard();System.out.println("dial2");

		System.out.println("dial2");
		CubeMessageDialog dial2 = new CubeMessageDialog(null, "Test ERROR: [àägqkpXXW] Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore ", "Test: [àägqkpXXW]",
				CubeMessageDialog.TYPE_ERROR);
		dial2.displayWizard();
		System.out.println("dial3");
		CubeMessageDialog dial3 = new CubeMessageDialog(null, "Test PLAIN: [àägqkpXXW] Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore ", "Test: [àägqkpXXW]",
				CubeMessageDialog.TYPE_PLAIN);
		dial3.displayWizard();
		System.out.println("dial4");
		CubeMessageDialog dial4 = new CubeMessageDialog(null, "Test WARNING: [àägqkpXXW] Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore ", "Test: [àägqkpXXW]",
				CubeMessageDialog.TYPE_WARNING);
		dial4.displayWizard();
		
		
		CubeConfirmationDialog dial5 = new CubeConfirmationDialog(null, "Test WARNING: àägqkpXXW ???  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore",
				CubeConfirmationDialog.TYPE_CANCEL_YES);		
		dial5.displayWizard();
		System.out.println(">>>>>> "+dial5.getDialogResult());
		System.out.println("done.");
		
	}
}
