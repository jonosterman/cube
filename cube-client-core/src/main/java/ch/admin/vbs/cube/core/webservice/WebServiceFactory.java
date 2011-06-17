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
package ch.admin.vbs.cube.core.webservice;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStore.Builder;

import javax.net.ssl.SSLSocketFactory;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;
import cube.cubemanager.services.CubeManagerServicePortType;

public class WebServiceFactory {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(WebServiceFactory.class);
	private final Builder builder;
	private SSLSocketFactory sslSocketFactory;
	private FiltersType filter;

	public WebServiceFactory(Builder builder) throws CubeException {
		this.builder = builder;
		// create custom SSLFactory
		KeyStore trustStore;
		if ("https".equals(CubeClientCoreProperties.getProperty("webservice.cubemanager.protocol"))) {
			try {
				trustStore = KeyStore.getInstance("jks");
				File truststore = new File(CubeClientCoreProperties.getProperty("rootca.keystore.file"));
				trustStore.load(new FileInputStream(truststore), CubeClientCoreProperties.getProperty("rootca.keystore.password").toCharArray());
				sslSocketFactory = CubeSSLSocketFactory.newSSLSocketFactory(this.builder, trustStore, false);
				// filters
				filter = new FiltersType();
				filter.getInclude().add(".*_EXPORT_.*");
				filter.getInclude().add(".*_EXPORT1024_.*");
				filter.getInclude().add(".*_WITH_DES_.*");
				filter.getInclude().add(".*_WITH_NULL_.*");
				filter.getExclude().add(".*_DH_anon_.*");
			} catch (Exception e) {
				throw new CubeException("Failed to init SSLSocketFactory", e);
			}
		}
	}

	/**
	 * Create webService based on properties file configuration.
	 * 
	 * @return
	 */
	public CubeManagerServicePortType createCubeManagerService() {
		// setup service client
		LOG.debug("Setup WebService Client");
		JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
		// debugging
		// factory.getInInterceptors().add(new LoggingInInterceptor(1000));
		// factory.getOutInterceptors().add(new LoggingOutInterceptor());
		// service
		factory.setServiceClass(CubeManagerServicePortType.class);
		//
		String url = String.format("%s://%s:%s/%s", //
				CubeClientCoreProperties.getProperty("webservice.cubemanager.protocol"), //
				CubeClientCoreProperties.getProperty("webservice.cubemanager.host"), //
				CubeClientCoreProperties.getProperty("webservice.cubemanager.port"), //
				CubeClientCoreProperties.getProperty("webservice.cubemanager.uri") //
				);
		LOG.debug("Connect webservice at [{}]", url);
		factory.setAddress(url);
		CubeManagerServicePortType srv = (CubeManagerServicePortType) factory.create();
		if ("https".equalsIgnoreCase(CubeClientCoreProperties.getProperty("webservice.cubemanager.protocol"))) {
			configureSSLOnTheClient(srv);
		}
		return srv;
	}

	private void configureSSLOnTheClient(Object c) {
		org.apache.cxf.endpoint.Client client = ClientProxy.getClient(c);
		LOG.debug("Customize client [{}]", client);
		HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
		try {
			// update client to use our custom SSLSocketFactory (using our
			// truststore and keystore)
			TLSClientParameters tlsClientParameters = new TLSClientParameters();
			tlsClientParameters.setSSLSocketFactory(sslSocketFactory);
			tlsClientParameters.setDisableCNCheck(false);
			tlsClientParameters.setCipherSuitesFilter(filter);
			httpConduit.setTlsClientParameters(tlsClientParameters);
		} catch (Exception exception) {
			LOG.error("Security configuration failed with the following: " + exception.getCause(), exception);
		}
	}
}
