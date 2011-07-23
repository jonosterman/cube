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

package ch.admin.vbs.cube.core.webservice;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.PKIXBuilderParameters;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.WebServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The <code>WebserviceSocketFactory</code> is a helper class to create socket
 * factories used by web clients.
 * 
 */
public class CubeSSLSocketFactory {
	private static final Log LOG = LogFactory.getLog(CubeSSLSocketFactory.class);

	/**
	 * Create a new SSL socket factory.
	 * 
	 * @param keyStoreBuilder
	 *            the key store builder
	 * @param trustStore
	 *            the trust store
	 * @param checkRevocation
	 *            <code>true</code> if certificate revocations should be
	 *            checked, else <code>false</code>
	 * @throws WebServiceException
	 *             if the creation failed
	 */
	public static SSLSocketFactory newSSLSocketFactory(KeyStore.Builder keyStoreBuilder, KeyStore trustStore, boolean checkRevocation)
			throws WebServiceException {
		KeyManagerFactory keyManagerFactory;
		try {
			keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
		} catch (NoSuchAlgorithmException e) {
			String message = "Unable to create key manager factory";
			LOG.error(message + ": " + e.getMessage());
			throw new WebServiceException(message, e);
		}
		KeyStoreBuilderParameters keyStoreBuilderParameters = new KeyStoreBuilderParameters(keyStoreBuilder);
		try {
			keyManagerFactory.init(keyStoreBuilderParameters);
		} catch (InvalidAlgorithmParameterException e) {
			String message = "Unable to initialize key manager factory";
			LOG.error(message + ": " + e.getMessage());
			throw new WebServiceException(message, e);
		}
		TrustManagerFactory trustManagerFactory;
		try {
			trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		} catch (NoSuchAlgorithmException e) {
			String message = "Unable to create trust manager factory";
			LOG.error(message + ": " + e.getMessage());
			throw new WebServiceException(message, e);
		}
		PKIXBuilderParameters pkixBuilderParameters;
		try {
			pkixBuilderParameters = new PKIXBuilderParameters(trustStore, null);
		} catch (KeyStoreException e) {
			String message = "The trust store is not initialized";
			LOG.error(message + ": " + e.getMessage());
			throw new WebServiceException(message, e);
		} catch (InvalidAlgorithmParameterException e) {
			String message = "The trust store does not contain any trusted certificate";
			LOG.error(message + ": " + e.getMessage());
			throw new WebServiceException(message, e);
		} catch (NullPointerException e) {
			String message = "The trust store is null";
			LOG.error(message + ": " + e.getMessage());
			throw new WebServiceException(message, e);
		}
		pkixBuilderParameters.setRevocationEnabled(checkRevocation);
		CertPathTrustManagerParameters certPathTrustManagerParameters = new CertPathTrustManagerParameters(pkixBuilderParameters);
		try {
			trustManagerFactory.init(certPathTrustManagerParameters);
		} catch (InvalidAlgorithmParameterException e) {
			String message = "Unable to initialize trust manager factory";
			LOG.error(message + ": " + e.getMessage());
			throw new WebServiceException(message, e);
		}
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e) {
			String message = "Unable to create SSL context";
			LOG.error(message + ": " + e.getMessage());
			throw new WebServiceException(message, e);
		}
		try {
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		} catch (KeyManagementException e) {
			String message = "Unable to initialize SSL context";
			LOG.error(message + ": " + e.getMessage());
			throw new WebServiceException(message, e);
		}
		SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		return sslSocketFactory;
	}
}