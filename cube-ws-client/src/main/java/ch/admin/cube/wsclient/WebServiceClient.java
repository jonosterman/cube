package ch.admin.cube.wsclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.IIdentityToken.KeyType;
import ch.admin.vbs.cube.cubemanage.CubeManage;
import ch.admin.vbs.cube.cubemanage.CubeManagePortType;

public class WebServiceClient implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(WebServiceClient.class);
	private static final long POOL_DELAY = 60000; // 60 secs.
	private boolean suspended;
	private final IIdentityToken id;
	private boolean pubkeySent;
	private Object poolLock = new Object();
	private Lock portLock = new ReentrantLock();
	private CubeManagePortType port;
	private boolean connected;
	private boolean running;

	public WebServiceClient(IIdentityToken id) {
		this.id = id;
	}

	public void start() {
		running = true;
		Thread t = new Thread(this, "WebServiceClient [" + id.getSubjectName() + "]");
		t.setDaemon(true);
		t.start();
	}

	public void stop() {
		running = false;
		synchronized (poolLock) {
			poolLock.notifyAll();
		}
	}

	public synchronized void setSuspended(boolean suspended) {
		if (this.suspended != suspended) {
			// state changed
			this.suspended = suspended;
			// pool
			synchronized (poolLock) {
				poolLock.notifyAll();
			}
		}
	}

	// ===============================================
	// WebService methods
	// ===============================================
	public void report(String message) {
		portLock.lock();
		try {
			if (port == null) {
				LOG.error("Failed to send report string since webservice is not connected (" + message + ")");
			} else {
				port.report(message, System.currentTimeMillis());
			}
		} finally {
			portLock.unlock();
		}
	}

	public void listVms() {
		portLock.lock();
		try {
			if (port == null) {
				LOG.error("Failed to list VMs since webservice is not connected.");
			} else {
				port.listVMs();
				// TODO decrypt VMs list and return arraylist
			}
		} finally {
			portLock.unlock();
		}
	}

	// ===============================================
	// Implements Runnables
	// ===============================================
	@Override
	public void run() {
		try {
			while (running) {
				if (!suspended && !connected && running) {
					// initialize webservice client (port)
					try {
						openHttps();
						connected = true;
					} catch (Exception e) {
						LOG.error("Failed to connect WebService", e);
						connected = false;
					}
				}
				synchronized (poolLock) {
					LOG.debug("Wait [{} ms] before tying to connecting webservice again",POOL_DELAY);
					poolLock.wait(POOL_DELAY);
				}
			}
		} catch (InterruptedException e) {
			LOG.error("Thread Failure", e);
		}
	}

	private void openHttps() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, FileNotFoundException, IOException,
			InvalidAlgorithmParameterException, KeyManagementException {
		String truststoreFile = CubeWsClientProperties.getProperty("cube.ws.truststore.JKS.file");
		char[] truststorePwd = CubeWsClientProperties.getProperty("cube.ws.truststore.JKS.password").toCharArray();
		// TrustStore & TrustManagerFactory
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		KeyStore ts = KeyStore.getInstance("JKS");
		ts.load(new FileInputStream(new File(truststoreFile)), truststorePwd);
		tmf.init(ts);
		// KeyStore & KeyManagerFactory
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
		Builder clientCrtBuilder = id.getBuilder();
		KeyStoreBuilderParameters keyStoreBuilderParameters = new KeyStoreBuilderParameters(clientCrtBuilder);
		kmf.init(keyStoreBuilderParameters);
		// SSL Context
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		// SocketFactory
		SSLSocketFactory factory = ctx.getSocketFactory();
		// test request
		// testHttpsRequest(factory); System.exit(0);
		// web service (use local wdsl since it will not work online with client
		// auth)
		CubeManage service = new CubeManage(getClass().getResource("/CubeManage.wsdl"));
		portLock.lock();
		try {
			port = service.getCubeManagePort();
			LOG.debug("Got WebService Port. Set SSL options.");
			// update proxy to enforce ssl client certificate
			Client proxy = ClientProxy.getClient(port);
			((BindingProvider) port).getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
			HTTPConduit conduit = (HTTPConduit) proxy.getConduit();
			TLSClientParameters tlsParam = new TLSClientParameters();
			tlsParam.setDisableCNCheck(false);
			tlsParam.setSSLSocketFactory(factory);
			addFilters(tlsParam);
			conduit.setTlsClientParameters(tlsParam);
			// load all certificates in token object
			if (!pubkeySent) {
				LOG.debug("Initial public key registration.");
				// WebService: login command is used to send our public
				// encryption key
				port.login(id.getCertificate(KeyType.ENCIPHERMENT).getEncoded());
				pubkeySent = true;
			}
			// WebService: report some message.
			port.report("connect", System.currentTimeMillis());
		} finally {
			portLock.unlock();
		}
	}

	private final void addFilters(TLSClientParameters tlsClientParameters) {
		/*
		 * these filters ensure that a ciphersuite with export-suitable or null
		 * encryption is used, but exclude anonymous Diffie-Hellman key change
		 * as this is vulnerable to man-in-the-middle attacks
		 */
		FiltersType filter = new FiltersType();
		filter.getInclude().add(".*_EXPORT_.*");
		filter.getInclude().add(".*_EXPORT1024_.*");
		filter.getInclude().add(".*_WITH_DES_.*");
		filter.getInclude().add(".*_WITH_AES_.*");
		filter.getInclude().add(".*_WITH_NULL_.*");
		filter.getExclude().add(".*_DH_anon_.*");
		tlsClientParameters.setCipherSuitesFilter(filter);
	}
}
