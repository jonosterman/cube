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

package ch.admin.vbs.cube.core.vbox;

import java.util.List;

import org.virtualbox_4_2.IMachine;
import org.virtualbox_4_2.IVirtualBox;
import org.virtualbox_4_2.VirtualBoxManager;

/**
 * This demo program try to configure a VM in VirtualBox using the web service
 * interface. The VirtualBox web service MUST be running (vboxwebsrv).
 */
public class DemoVboxWebServiceConnect {
	public static void main(String[] args) throws Exception {
		DemoVboxWebServiceConnect demo = new DemoVboxWebServiceConnect();
		demo.test1();
	}

	private void test1() throws Exception {
		VirtualBoxManager mgr = VirtualBoxManager.createInstance(null);
		mgr.connect("http://localhost:18083", "", "");
		IVirtualBox vbox = mgr.getVBox();
		List<IMachine> m = vbox.getMachines();
		System.out.println("[" + m.size() + "] machines");
	}
}
