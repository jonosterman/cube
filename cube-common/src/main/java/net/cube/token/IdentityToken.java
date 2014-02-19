package net.cube.token;

import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.cube.common.CubeConfig;
import net.cube.common.crypto.HashUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityToken implements IIdentityToken {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(IdentityToken.class);
	private final KeyStore keystore;
	protected HashMap<KeyType, LinkedList<X509Certificate>> certificates = new HashMap<IIdentityToken.KeyType, LinkedList<X509Certificate>>();
	protected String uuid; // <-- Subject DN
	protected String uuidHash;
	protected String simpleName;
	private byte[] password;
	private byte[] xor;
	private final Builder builder;
	private static SecureRandom rnd = new SecureRandom();

	public IdentityToken(KeyStore keystore, Builder builder, char[] password) {
		this.keystore = keystore;
		this.builder = builder;
		hidePassword(password);
		try {
			Enumeration<String> aliases = keystore.aliases();
			while (aliases != null && aliases.hasMoreElements()) {
				String next = aliases.nextElement();
				X509Certificate cert = (X509Certificate) keystore.getCertificate(next);
				// check subject DN
				if (uuid == null) {
					uuid = cert.getSubjectDN().getName();
					uuidHash = HashUtil.sha512UrlInBase64(uuid); // <- long
					// uuidHash = HashUtil.md5(uuid); // <- short (human
					// readable)
					simpleName = extractName(uuid); // extract user name from DN
				} else if (!uuid.equals(cert.getSubjectDN().getName())) {
					throw new RuntimeException("This smart-card contains certificates with different subject DN. This is not allowed! [" + uuid + "]!=["
							+ cert.getSubjectDN().getName() + "]");
				}
				// check certificate
				boolean[] keyUsage = cert.getKeyUsage();
				if (keyUsage == null) {
					LOG.warn("Missing key usage for alias [{}]", next);
				}
				List<String> extKeyUsage = cert.getExtendedKeyUsage();
				if (extKeyUsage == null) {
					LOG.warn("Missing extended key usage for alias [{}]", next);
				}
				// determine certificate's type
				if (keyUsage != null && !extKeyUsage.isEmpty()) {
					if (keyUsage[0] && keyUsage[1] && !keyUsage[2] && !keyUsage[3]) {
						// User Signature : Key Usage [1,1,0,0,0,0,0,0,0]
						push(next, cert, KeyType.SIGNATURE);
					} else if (!keyUsage[0] && !keyUsage[1] && keyUsage[2] && keyUsage[3]) {
						// User Encipherment : Key Usage [0,0,1,1,0,0,0,0,0]
						push(next, cert, KeyType.ENCIPHERMENT);
					} else if (keyUsage[0] && !keyUsage[1] && !keyUsage[2] && !keyUsage[3]) {
						// User Authentication : Key Usage [1,0,0,0,0,0,0,0,0]
						push(next, cert, KeyType.AUTHENTIFICATION);
					} else {
						// Unknown
						LOG.warn("Unknown key usage [{}]:", String.format("%d,%d,%d,%d,%d,%d,%d,%d,%d", //
								keyUsage[0] ? 1 : 0, //
								keyUsage[1] ? 1 : 0, //
								keyUsage[2] ? 1 : 0, //
								keyUsage[3] ? 1 : 0, //
								keyUsage[4] ? 1 : 0, //
								keyUsage[5] ? 1 : 0, //
								keyUsage[6] ? 1 : 0, //
								keyUsage[7] ? 1 : 0, //
								keyUsage[8] ? 1 : 0 //
								));
					}
				}
			}
		} catch (RuntimeException e) {
			LOG.error("Problem retrievin the subject alias.", e);
		} catch (KeyStoreException e) {
			LOG.error("Problem retrievin the subject alias.", e);
		} catch (CertificateParsingException e) {
			LOG.error("Problem retrievin the subject alias.", e);
		}
	}

	/**
	 * hide/show_password are used to keep a COPY of the password in a
	 * not-so-easy-to-read form in memory
	 */
	private final void hidePassword(char[] clear) {
		this.password = new byte[clear.length];
		this.xor = new byte[clear.length];
		// generate random mask
		rnd.nextBytes(this.xor);
		// store password mixed with mask
		for (int i = 0; i < clear.length; i++) {
			this.password[i] = (byte) (clear[i] ^ xor[i]);
		}
	}

	private char[] showPassword() {
		char[] clear = new char[password.length];
		for (int i = 0; i < clear.length; i++) {
			clear[i] = (char) (password[i] ^ xor[i]);
		}
		return clear;
	}

	/** push certificate in local cache. Ensure the valid one is at the top. */
	private void push(String alias, X509Certificate cert, KeyType type) {
		LOG.debug("Found certificate of type [{}] named [{}]", type, alias);
		LinkedList<X509Certificate> list = certificates.get(type);
		// lazy initialization
		if (list == null) {
			list = new LinkedList<X509Certificate>();
			certificates.put(type, list);
		}
		// store certificate
		if (list.size() == 0) {
			list.add(cert);
		} else {
			// check if the given certificate is newer as the one in the list
			X509Certificate top = list.getFirst();
			if (top.getNotAfter().before(cert.getNotAfter())) {
				// 'cert' is valid after 'top' -> replace on top of list
				list.addFirst(cert);
				LOG.debug("This certificate is newer that previous one");
			} else {
				// 'cert' is NOT valid after 'top' -> append at the end of list
				list.addLast(cert);
				LOG.debug("This certificate is older that previous one");
			}
		}
	}

	@Override
	public PrivateKey getPrivatekey(KeyType type) {
		LinkedList<X509Certificate> list = certificates.get(type);
		try {
			return list == null || list.size() == 0 ? null : ((KeyStore.PrivateKeyEntry) keystore.getEntry(keystore.getCertificateAlias(list.peek()), new PasswordProtection(
					showPassword()))).getPrivateKey();
		} catch (NoSuchAlgorithmException e) {
			LOG.error("Failed to retrieve private key.", e);
		} catch (UnrecoverableEntryException e) {
			LOG.error("Failed to retrieve private key.", e);
		} catch (KeyStoreException e) {
			LOG.error("Failed to retrieve private key.", e);
		}
		return null;
	}

	@Override
	public PublicKey getPublickey(KeyType type) {
		LinkedList<X509Certificate> list = certificates.get(type);
		return list == null || list.size() == 0 ? null : list.peek().getPublicKey();
	}

	@Override
	public X509Certificate getCertificate(KeyType type) {
		LinkedList<X509Certificate> list = certificates.get(type);
		return list == null || list.size() == 0 ? null : list.peek();
	}

	@Override
	public PrivateKey[] getAllPrivatekey(KeyType type) {
		ArrayList<PrivateKey> keys = new ArrayList<PrivateKey>();
		LinkedList<X509Certificate> list = certificates.get(type);
		if (list != null) {
			for (X509Certificate c : list) {
				try {
					keys.add(((KeyStore.PrivateKeyEntry) keystore.getEntry(keystore.getCertificateAlias(c), new PasswordProtection(showPassword()))).getPrivateKey());
				} catch (NoSuchAlgorithmException e) {
					LOG.error("Failed to retrieve private key.", e);
				} catch (UnrecoverableEntryException e) {
					LOG.error("Failed to retrieve private key.", e);
				} catch (KeyStoreException e) {
					LOG.error("Failed to retrieve private key.", e);
				}
			}
		}
		return keys.toArray(new PrivateKey[keys.size()]);
	}

	@Override
	public PublicKey[] getAllPublickey(KeyType type) {
		ArrayList<PublicKey> keys = new ArrayList<PublicKey>();
		LinkedList<X509Certificate> list = certificates.get(type);
		if (list != null) {
			for (X509Certificate c : list) {
				keys.add(c.getPublicKey());
			}
		}
		return keys.toArray(new PublicKey[keys.size()]);
	}

	@Override
	public String getSubjectName() {
		return simpleName;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public String getUuidHash() {
		return uuidHash;
	}

	/**
	 * Extracts a name from a char sequence.
	 * 
	 * @param charSequence
	 *            the char sequence from which the name gets extracted.
	 * @return return the extract name.
	 */
	private static String extractName(String charSequence) {
		String regex = CubeConfig.getProperty("token.userRegexInCommonNameUrl");
		if (regex == null) {
			LOG.debug("do not extract user name (no regexp provided in configuration file) -> [" + charSequence + "]");
			return charSequence;
		}
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(charSequence);
		if (m.find()) {
			charSequence = m.group(1);
		} else {
			charSequence = "";
		}
		return charSequence;
	}

	@Override
	public Builder getBuilder() {
		return builder;
	}
}
