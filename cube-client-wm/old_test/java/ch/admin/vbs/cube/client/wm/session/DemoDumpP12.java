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

package ch.admin.vbs.cube.client.wm.session;

import java.io.File;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.IAuthModuleListener;
import ch.admin.vbs.cube.core.impl.P12AuthModule;

public class DemoDumpP12 {
	private static final String P12_PASSWORD = "111222";
	

	public static void main(String[] args) throws Exception {
		Object obj = new Object();
		P12AuthModule auth = new P12AuthModule(new File(obj.getClass().getResource("/cube-01_pwd-is-111222.p12").getFile()));
		auth.start();
		auth.addListener(new IAuthModuleListener() {
			@Override
			public void notifyAuthModuleEvent(AuthModuleEvent event) {
				try {
					KeyStore keystore = event.getKeystore();
					// dump :::
					System.out.println("Dump token:");
					System.out.println("Size: "+keystore.size());
					System.out.println("Type: "+keystore.getType());
					
					Enumeration<String> aliases = keystore.aliases();
					while (aliases.hasMoreElements()) {
						String next = aliases.nextElement();
						System.out.println("Alias: [" + next + "]");
						X509Certificate cert = (X509Certificate) keystore.getCertificate(next);
						System.out.printf("Public  Key: [%s][%s][%d]\n",cert.getPublicKey().getAlgorithm(),cert.getPublicKey().getFormat(),cert.getPublicKey().getEncoded().length);
						Key priv = keystore.getKey(next, P12_PASSWORD.toCharArray());
						System.out.printf("Private Key: [%s][%s][%d]\n",priv.getAlgorithm(),priv.getFormat(),priv.getEncoded().length);
						System.out.printf("Issuer DN: [%s]\n", cert.getIssuerDN().getName());
						System.out.printf("Subject DN: [%s]\n", cert.getSubjectDN().getName());
						boolean[] keyUsage = cert.getKeyUsage();
						System.out.printf("Key Usage [%d,%d,%d,%d,%d,%d,%d,%d,%d]\n", //
								keyUsage[0] ? 1 : 0, //
								keyUsage[1] ? 1 : 0, //
								keyUsage[2] ? 1 : 0, //
								keyUsage[3] ? 1 : 0, //
								keyUsage[4] ? 1 : 0, //
								keyUsage[5] ? 1 : 0, //
								keyUsage[6] ? 1 : 0, //
								keyUsage[7] ? 1 : 0, //
								keyUsage[8] ? 1 : 0 //
								);
						// ext
						for (String ext : cert.getExtendedKeyUsage()) {
							System.out.printf("Extended Usage: [%s]\n", ext);
						}
					}
					System.exit(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		auth.openToken();
		auth.setPassword(P12_PASSWORD.toCharArray());
	}
}
