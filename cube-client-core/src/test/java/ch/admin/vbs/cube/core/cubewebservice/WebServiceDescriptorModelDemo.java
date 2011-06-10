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

package ch.admin.vbs.cube.core.cubewebservice;

import java.io.File;
import java.security.KeyStore.Builder;

import javax.swing.JOptionPane;

import org.junit.Ignore;
import org.junit.Test;

import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.IAuthModuleListener;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;
import ch.admin.vbs.cube.core.impl.ScAuthModule;
import ch.admin.vbs.cube.core.vm.VmModel;
import ch.admin.vbs.cube.core.vm.list.DescriptorModelCache;
import ch.admin.vbs.cube.core.vm.list.WSDescriptorUpdater;

public class WebServiceDescriptorModelDemo {
	public static void main(String[] args) throws Exception {
		WebServiceDescriptorModelDemo d = new WebServiceDescriptorModelDemo();
		d.testWebServiceDescriptorModel();
	}

	@Test
	@Ignore
	private void testWebServiceDescriptorModel() throws Exception {
		// / init smartcard stuff
		ScAuthModule auth = new ScAuthModule();
		auth.start();
		auth.openToken();
		auth.addListener(new IAuthModuleListener() {
			@Override
			public void notifyAuthModuleEvent(AuthModuleEvent event) {
				if (event.getType() == AuthEventType.SUCCEED) {
					try {
						connectWS(event.getBuilder());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Authentication failed");
				}
			}
		});
		auth.setPassword(JOptionPane.showInputDialog("PIN").toCharArray());
	}

	private void connectWS(Builder builder) throws Exception {
		File tmp = new File(new File(System.getProperty("java.io.tmpdir")), "tmp-cache");
		//
		VmModel model = new VmModel();
		DescriptorModelCache cache = new DescriptorModelCache(model, tmp);
		WSDescriptorUpdater wsu = new WSDescriptorUpdater(model, builder);
		cache.start(); // load cache
		wsu.start(); // start connecting wS
		Thread.sleep(5000);
		System.out.println("done.");
	}
}
