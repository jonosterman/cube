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

package ch.admin.vbs.cube.core.impl;

import java.io.File;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.CubeClientCoreProperties;

/**
 * Validates all keystore's certificate chains against a locally available root
 * certificate.
 */
public class CaValidation {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(CaValidation.class);
	private KeyStore keystore;
	private X509Certificate caCert;

	public CaValidation() {
		String caFile = CubeClientCoreProperties.getProperty("rootca.keystore.file");
		char[] caPwd = CubeClientCoreProperties.getProperty("rootca.keystore.password").toCharArray();
		Builder builder = KeyStore.Builder.newInstance("JKS", null, new File(caFile), new KeyStore.PasswordProtection(caPwd));
		try {
			keystore = builder.getKeyStore();
			// keystore MUST only have one certificate
			String alias = keystore.aliases().nextElement();
			caCert = (X509Certificate) keystore.getCertificate(alias);
			LOG.info("Use root certificate [{}]", caCert.getSubjectDN());
		} catch (KeyStoreException e) {
			LOG.error("Failed to load CA keystore", e);
		}
	}

	public boolean validate(KeyStore keystore) {
		int aliasCount = 0;
		int verifiedAliasCount = 0;
		try {
			// validate all certificates of the keystore
			Enumeration<String> en = keystore.aliases();
			LOOP: while (en.hasMoreElements()) {
				String alias = en.nextElement();
				aliasCount++;
				LOG.debug("Verify [{}]", alias);
				Certificate[] chain = keystore.getCertificateChain(alias);
				for (int i = 0; i < chain.length - 1; i++) {
					X509Certificate xcert = (X509Certificate) chain[i];
					if (xcert.getIssuerDN().getName().equals(caCert.getSubjectDN().getName())) {
						LOG.debug("> root  : verify [{}] with [{}]", xcert.getSubjectDN(), "<Local Root CA>");
						xcert.verify(caCert.getPublicKey());
						LOG.debug("Certificate Chain OK.");
						verifiedAliasCount++;
						continue LOOP;
					} else {
						LOG.debug("> chain : verify [{}] with [{}]", xcert.getSubjectDN(), ((X509Certificate) chain[i + 1]).getSubjectDN().getName());
						xcert.verify(chain[i + 1].getPublicKey());
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to validate keystore", e);
		}
		if (aliasCount != verifiedAliasCount) {
			LOG.error("Failed to validate all certificates of keystore [{} / {}]", verifiedAliasCount, aliasCount);
		}
		return aliasCount == verifiedAliasCount;
	}
}
