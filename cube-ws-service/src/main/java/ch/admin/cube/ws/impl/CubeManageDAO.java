package ch.admin.cube.ws.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.util.DerValue;
import sun.security.x509.X509Key;

import ch.admin.cube.ws.CubeWsServiceProperties;
import ch.admin.cube.ws.ICubeManageDAO;
import ch.admin.vbs.cube.common.crypto.Base64;
import ch.admin.vbs.cube.common.crypto.HashUtil;

public class CubeManageDAO implements ICubeManageDAO {
	private static final Logger LOG = LoggerFactory.getLogger(CubeManageDAO.class);
	private File baseDir;
	private Lock lock = new ReentrantLock();

	public CubeManageDAO() {
		baseDir = new File(CubeWsServiceProperties.getProperty("cube.manage.datastore.path"));
	}

	private Properties loadStore() throws IOException {
		File storeFile = new File(baseDir, "pubkeys.xml");
		Properties store = new Properties();
		if (storeFile.exists()) {
			FileInputStream fis = new FileInputStream(storeFile);
			store.loadFromXML(fis);
			fis.close();
		} else {
			LOG.debug("No Public Key Stroe found.");
		}		
		return store;
	}
	
	private void saveStore(Properties store) throws IOException {
		File storeFile = new File(baseDir, "pubkeys.xml");
		FileOutputStream fos = new FileOutputStream(storeFile);
		store.storeToXML(fos, "Public Keys List");
		fos.flush();
		fos.close();
	}
	
	// ====================================
	// ICubeManageDAO
	// ====================================
	@Override
	public void storePublicKey(String dn, java.security.PublicKey publicKey) {
		lock.lock();
		try {
			// check/create user directory
			String dnHash = HashUtil.md5(dn);
			File userDir = new File(baseDir, dnHash);
			if (!userDir.exists()) {
				userDir.mkdirs();
			}
			// load public key store
			Properties store = loadStore();
			// check if current key is already known
			byte[] raw = publicKey.getEncoded();
			if (store.containsKey(dn)) {
				// load ol key and compare
				LOG.debug("Load old key..");
				byte[] old = (byte[]) Base64.decode((String)store.get(dn));
				//PublicKey oldpk = X509Key.parse(new DerValue(old));
				//LOG.debug("   .. " + oldpk);
				if (Arrays.equals(old, raw)) {
					// already known keys.
				} else {
					// key for user has changed. store old one under another name.					
					store.put(String.format("%1$tY%1$tm%1$td-%s", new Date(), dn), Base64.encodeBytes(old));
					// store new one
					store.put(dn, Base64.encodeBytes(raw));					
					//
					LOG.debug("key has been updated [{}]", dn);
				}
			} else {
				LOG.debug("Add new public key [{}] [" + raw + "]", dn);
				store.put(dn, Base64.encodeBytes(raw));
			}
			saveStore(store);
			
		} catch (Exception e) {
			LOG.error("Failed to store public key.", e);
		} finally {
			lock.unlock();
		}
	}
}
