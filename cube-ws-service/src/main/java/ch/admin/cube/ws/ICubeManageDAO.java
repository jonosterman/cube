package ch.admin.cube.ws;

import java.security.PublicKey;

public interface ICubeManageDAO {
	public void storePublicKey(String dn, PublicKey publicKey);

}
