package ch.admin.cube.ws;

import java.io.File;
import java.security.cert.X509Certificate;

public interface ICubeManageDAO {
	public void storePublicKey(X509Certificate x509);

	public void report(X509Certificate x509, String message, long timestamp);

	public File listVMs(X509Certificate x509);

}
