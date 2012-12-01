/*
 * @(#)SSLSocketClientWithClientAuth.java	1.5 01/05/10
 *
 * Copyright 1994-2004 Oracle and/or its affiliates. All rights reserved. 
 *
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following 
 * conditions are met: 
 * 
 * -Redistribution of source code must retain the above copyright 
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistribution in binary form must reproduce the above copyright 
 * notice, this list of conditions and the following disclaimer in 
 * the documentation and/or other materials provided with the 
 * distribution. 
 * 
 * Neither the name of Oracle and/or its affiliates. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY 
 * EXCLUDED. Oracle and/or its affiliates. ("SUN") AND ITS LICENSORS SHALL 
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT 
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS 
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR 
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, 
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER 
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF 
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS 
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. 
 * 
 * You acknowledge that this software is not designed, licensed or 
 * intended for use in the design, construction, operation or 
 * maintenance of any nuclear facility. 
 */


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.KeyStore.Builder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/*
 * This example shows how to set up a key manager to do client
 * authentication if required by server.
 *
 * This program assumes that the client is not inside a firewall.
 * The application can be modified to connect to a server outside
 * the firewall by following SSLSocketClientWithTunneling.java.
 */
public class SSLSocketClientWithClientAuth {

    public static void main(String[] args) throws Exception {
	String host = null;
	int port = -1;
	String path = null;
	
    host = "server.cube.com";
    port = 8443;
    path = "/cube-server/services";

	
	try {

	    /*
	     * Set up a key manager for client authentication
	     * if asked by the server.  Use the implementation's
	     * default TrustStore and secureRandom routines.
	     */
	    SSLSocketFactory factory = null;
	    try {
		SSLContext ctx;
		KeyManagerFactory kmf;
		TrustManagerFactory tmf;
		KeyStore ks,ts;
		char[] passphrase = "123456".toCharArray();

		
		ctx = SSLContext.getInstance("TLS");
		//
		kmf = KeyManagerFactory.getInstance("NewSunX509");
		
		Builder clientCrtBuilder = KeyStore.Builder.newInstance("PKCS12", null,
				new File("/home/manhattan/dev/cube/cube-ws-service/keystuff/client/client.p12"), new KeyStore.PasswordProtection("123456".toCharArray()));
		KeyStoreBuilderParameters keyStoreBuilderParameters = new KeyStoreBuilderParameters(clientCrtBuilder);
		kmf.init(keyStoreBuilderParameters);
		
		//
		tmf = TrustManagerFactory.getInstance("SunX509");
		ts = KeyStore.getInstance("JKS");
		ts.load(new FileInputStream("/home/manhattan/dev/cube/cube-ws-service/keystuff/truststore.jks"), passphrase);

		//kmf.init(ks, passphrase);
		tmf.init(ts);
		ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		factory = ctx.getSocketFactory();
	    } catch (Exception e) {
		throw new IOException(e.getMessage());
	    }

	    SSLSocket socket = (SSLSocket)factory.createSocket(host, port);

	    /*
	     * send http request
	     *
	     * See SSLSocketClient.java for more information about why
	     * there is a forced handshake here when using PrintWriters.
	     */
	    socket.startHandshake();

	    PrintWriter out = new PrintWriter(
				  new BufferedWriter(
				  new OutputStreamWriter(
     				  socket.getOutputStream())));
	    out.println("GET " + path + " HTTP/1.0");
	    out.println();
	    out.flush();

	    /*
	     * Make sure there were no surprises
	     */
	    if (out.checkError())
		System.out.println(
		    "SSLSocketClient: java.io.PrintWriter error");

	    /* read response */
	    BufferedReader in = new BufferedReader(
				    new InputStreamReader(
				    socket.getInputStream()));

	    String inputLine;

	    while ((inputLine = in.readLine()) != null)
		System.out.println(inputLine);

	    in.close();
	    out.close();
	    socket.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
