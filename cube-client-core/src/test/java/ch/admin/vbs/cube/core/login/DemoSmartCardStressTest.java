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

package ch.admin.vbs.cube.core.login;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.Security;
import java.util.GregorianCalendar;
import java.util.logging.Logger;


import sun.security.pkcs11.SunPKCS11;
import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;

public class DemoSmartCardStressTest {
	private static final Logger LOG = Logger.getLogger("DemoSmartCardStressTest");
	private static final String SC_PKCS11_LIBRARY_PROPERTY = "SCAdapter.pkcs11Library";
	private SunPKCS11 provider;
	private Builder builder;
	private KeyStore keystore;

	public static void main(String[] args) throws Exception {
		DemoSmartCardStressTest d = new DemoSmartCardStressTest();
		for (int i = 0; i < 1000; i++) {
			d.start(i);
		}
	}

	private void start(int i) throws CubeException, Exception {
		String pkcs11LibraryPath = CubeClientCoreProperties.getProperty(SC_PKCS11_LIBRARY_PROPERTY);
		LOG.info("Initialize SunPKCS11 ["+i+"]");
		if (provider != null) {
			Security.removeProvider(provider.getName());
		}
		long t2 = System.currentTimeMillis();
		StringBuilder buf = new StringBuilder();
		buf.append("library = ").append(pkcs11LibraryPath).append("\nname = Cube\n");
		provider = new sun.security.pkcs11.SunPKCS11(new ByteArrayInputStream(buf.toString().getBytes()));
		Security.addProvider(provider);
		t2 = System.currentTimeMillis() - t2;
		long t1 = System.currentTimeMillis();
		// create builder
		builder = KeyStore.Builder.newInstance("PKCS11", provider, new KeyStore.PasswordProtection("111222".toCharArray()));
		// request keystore
		t1 = System.currentTimeMillis() - t1;
		long t0 = System.currentTimeMillis();
		GregorianCalendar now = new GregorianCalendar();
		LOG.info("Request keystore ["+String.format("%1$tH:%1$tM:%1$tS", now)+"]");
		keystore = builder.getKeyStore();// <- slow part
		t0 = System.currentTimeMillis() - t0;
		LOG.info("Keystore created ["+t2+" ms]["+t1+" ms]["+t0+" ms].");
	}
	
	
	
}
