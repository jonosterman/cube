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

import ch.admin.vbs.cube.client.wm.ui.dialog.ButtonLessDialog;

public class CubeDialogButtonLessDemo {
	public static void main(String[] args) {
		CubeDialogButtonLessDemo demo = new CubeDialogButtonLessDemo();
		demo.testDialog();
	}

	@Test
	@Ignore
	public void testDialog() {
		System.out.println("dial1");
		
		ButtonLessDialog dial1 = new ButtonLessDialog(null, "Test INFO: [àägqkpXXW]");
		dial1.displayWizard();

		
		System.out.println("done.");
	}
}
