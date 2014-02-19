package net.cube.token;

import java.security.KeyStore.Builder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public interface IIdentityToken {
	enum KeyType {
		ENCIPHERMENT, SIGNATURE, AUTHENTIFICATION
	}

	/** @return last valid private key */
	PrivateKey getPrivatekey(KeyType type);

	/** @return last valid public key */
	PublicKey getPublickey(KeyType type);

	/** @return last all private key (some may be expired) */
	PrivateKey[] getAllPrivatekey(KeyType type);

	/** @return last all public key (some may be expired) */
	PublicKey[] getAllPublickey(KeyType type);

	/** @return a human readable subject's name */
	String getSubjectName();

	/**
	 * @return a unique string identifier for this USER (even if it replace or
	 *         renew its token)
	 */
	String getUuid();

	/** @return a one-way hash of the user's uuid. */
	String getUuidHash();

	/** needed to build SSL connections. */
	Builder getBuilder();

	X509Certificate getCertificate(KeyType type);
}