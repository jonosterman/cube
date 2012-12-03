package ch.admin.cube.ws.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.cube.ws.CubeWsServiceProperties;
import ch.admin.cube.ws.ICubeManageDAO;
import ch.admin.vbs.cube.common.crypto.HashUtil;

public class CubeManageDAO implements ICubeManageDAO {
	private static final Logger LOG = LoggerFactory.getLogger(CubeManageDAO.class);
	private File baseDir;
	private Lock lock = new ReentrantLock();

	public CubeManageDAO() {
		baseDir = new File(CubeWsServiceProperties.getProperty("cube.manage.datastore.path"));
	}

	// ====================================
	// ICubeManageDAO
	// ====================================
	@Override
	public void storePublicKey(String dn, java.security.PublicKey publicKey) {
		lock.lock();
		try {
			// check/create user directory
			String dnHash = HashUtil.sha256UrlInBase64(dn);
			File userDir = new File(baseDir, dnHash);
			if (!userDir.exists()) {
				userDir.mkdirs();
			}
			// store public key
			File pubkeyFile = new File(baseDir, "pubkeys.xml");
			Properties pubkeys = new Properties();
			if (pubkeyFile.exists()) {
				FileInputStream fis = new FileInputStream(pubkeyFile);
				pubkeys.loadFromXML(fis);
				fis.close();
			}
			pubkeys.put(dn, publicKey.getEncoded());
			FileOutputStream fos = new FileOutputStream(pubkeyFile);
			pubkeys.storeToXML(fos, "Public Keys List");
			fos.close();
		} catch (Exception e) {
		} finally {
			lock.unlock();
		}
	}
	
	
	
}
