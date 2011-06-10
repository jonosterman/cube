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

package ch.admin.vbs.cube.client.wm.session;

import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.swing.JOptionPane;

import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.IAuthModuleListener;
import ch.admin.vbs.cube.core.impl.ScAuthModule;

public class DemoDumpSC {
	public static void main(String[] args) {
		ScAuthModule auth = new ScAuthModule();
		auth.start();
		auth.addListener(new IAuthModuleListener() {
			@Override
			public void notifyAuthModuleEvent(AuthModuleEvent event) {
				try {
					System.out.println("" + event.getType());
					KeyStore keystore = event.getKeystore();
					// dump :::
					System.out.println("Dump token:");
					System.out.println("Size: "+keystore.size());
					System.out.println("Type: "+keystore.getType());
					Enumeration<String> aliases = keystore.aliases();
					while (aliases.hasMoreElements()) {
						String next = aliases.nextElement();
						System.out.println("----------------------------------------");
						System.out.println("Alias: [" + next + "]");
						X509Certificate cert = (X509Certificate) keystore.getCertificate(next);
						//
						
						//
						System.out.printf("Validity: [%1$td.%1$tm.%1$tY %1$tH:%1$tM]-[%2$td.%2$tm.%2$tY %2$tH:%2$tM]\n",cert.getNotBefore(),cert.getNotAfter());
						System.out.printf("Public  Key: [%s][%s][%d]\n",cert.getPublicKey().getAlgorithm(),cert.getPublicKey().getFormat(),cert.getPublicKey().getEncoded().length);
						Key priv = keystore.getKey(next, null);
						System.out.printf("Private Key: [%s][%s][%d]\n",priv.getAlgorithm(),priv.getFormat(),priv.getEncoded());
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
		auth.setPassword(JOptionPane.showInputDialog("PIN").toCharArray());
	}
}
