package demo.net.cube.wsclient;

import java.io.File;
import java.security.KeyStoreException;

import net.cube.token.IIdentityToken;
import net.cube.token.IdentityToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.cube.wsclient.WebServiceClient;

public class WebserviceClientDemo {
	private static final Logger LOG = LoggerFactory.getLogger(WebserviceClientDemo.class);

	public WebserviceClientDemo() {
	}

	public static void main(String[] args) throws Exception {
		WebserviceClientDemo c = new WebserviceClientDemo();
		c.start();
	}

	private void start() throws Exception {
		IIdentityToken id = IdentityToken.create(new File(System.getProperty("user.home")+"/cube-pki/client0.jks"), "123456".toCharArray());
		WebServiceClient cli = new WebServiceClient(id);
		cli.start();
		while(!cli.isConnected()) {
			Thread.sleep(1000);
			System.out.println("...");
		}
		System.out.println("connected!");
		cli.listVms();
		cli.stop();
		
		
	}
}
