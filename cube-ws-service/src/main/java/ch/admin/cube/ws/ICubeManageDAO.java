package ch.admin.cube.ws;

import java.security.cert.X509Certificate;

public interface ICubeManageDAO {
	public void storePublicKey(X509Certificate x509);

}
