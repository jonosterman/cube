package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.activation.DataHandler;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.example.contract.cubemanage.CubeManagePortType;
import org.example.contract.cubemanage.CubeManageService;
import org.example.schema.cubemanage.SomeParamComplex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	
public class WSClient {
	private static final Logger LOG = LoggerFactory.getLogger(WSClient.class);

	public WSClient() {
	}

	public void https() throws Exception {
		// TrustStore & TrustManagerFactory
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		KeyStore ts = KeyStore.getInstance("JKS");
		ts.load(new FileInputStream(new File(System.getProperty("user.home"), "cube-pki/truststore.jks")), "123456".toCharArray());
		tmf.init(ts);
		// KeyStore & KeyManagerFactory
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
		Builder clientCrtBuilder = KeyStore.Builder.newInstance("PKCS12", //
				null, //
				new File(System.getProperty("user.home"), "cube-pki/client0.p12"), //
				new KeyStore.PasswordProtection("123456".toCharArray()));
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
		CubeManageService service = new CubeManageService(getClass().getResource("/CubeManage.wsdl"));
		CubeManagePortType port = service.getCubeManagePort();
		//
		Client proxy = ClientProxy.getClient(port);
		((BindingProvider) port).getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
		HTTPConduit conduit = (HTTPConduit) proxy.getConduit();
		TLSClientParameters tlsParam = new TLSClientParameters();
		tlsParam.setDisableCNCheck(false);
		tlsParam.setSSLSocketFactory(factory);
		addFilters(tlsParam);
		conduit.setTlsClientParameters(tlsParam);
		//
		port.login();
		//
		port.report("login", System.currentTimeMillis());
		//
		doubleIt(port, 10);
		DataHandler dh = port.listVMs();
		ZipInputStream zis = new ZipInputStream(dh.getInputStream());
		while (zis.available() > 0) {
			ZipEntry entry = zis.getNextEntry();
			System.out.println("Entry [" + entry.getName() + "] <<");
		}
		//
		port.report("logout", System.currentTimeMillis());
	}

	private void addFilters(TLSClientParameters tlsClientParameters) {
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

	private void testHttpsRequest(SSLSocketFactory factory) throws UnknownHostException, IOException {
		// test request
		SSLSocket socket = (SSLSocket) factory.createSocket("server.cube.com", 8443);
		/*
		 * send http request
		 * 
		 * See SSLSocketClient.java for more information about why there is a
		 * forced handshake here when using PrintWriters.
		 */
		socket.startHandshake();
		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
		out.println("GET /cube-server/services HTTP/1.0");
		out.println();
		out.flush();
		/*
		 * Make sure there were no surprises
		 */
		if (out.checkError())
			System.out.println("SSLSocketClient: java.io.PrintWriter error");
		/* read response */
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String inputLine;
		while ((inputLine = in.readLine()) != null)
			System.out.println(inputLine);
		in.close();
		out.close();
		socket.close();
	}

	public static void main(String[] args) throws Exception {
		WSClient c = new WSClient();
		// System.out.println("## HTTP ############");
		// c.http();
		System.out.println("## HTTPS ###########");
		c.https();
	}

	public static void doubleIt(CubeManagePortType port, int numToDouble) {
		SomeParamComplex p = new SomeParamComplex();
		p.setMachine("blah");
		p.setSize(32);
		int resp = port.tripleIt(p);
		System.out.println("The number " + numToDouble + " doubled is " + resp);
	}
}
