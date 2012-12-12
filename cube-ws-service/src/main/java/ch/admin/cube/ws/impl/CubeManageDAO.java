package ch.admin.cube.ws.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.provider.X509Factory;
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

	private void writePemFile(File pemFile, X509Certificate x509) throws IOException, CertificateEncodingException {
		FileWriter fw = new FileWriter(pemFile);
		fw.write(X509Factory.BEGIN_CERT + '\n');
		fw.write(Base64.encodeBytes(x509.getEncoded()));
		fw.write('\n' + X509Factory.END_CERT + '\n');
		fw.close();
	}

	private X509Certificate readPemFile(File pemFile) throws CertificateException, FileNotFoundException {
		// decode
		CertificateFactory fact = CertificateFactory.getInstance("X.509");
		return (X509Certificate) fact.generateCertificate(new FileInputStream(pemFile));
	}

	private void log(X509Certificate x509, String message) throws IOException {
		log(x509, message, System.currentTimeMillis());
	}

	private void log(X509Certificate x509, String message, long timestamp) throws IOException {
		String dn = x509.getSubjectDN().getName();
		String dnHash = HashUtil.sha512UrlInBase64(dn);
		// log into user directory
		File log = new File(new File(baseDir, dnHash), "report.log");
		GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		c.setTimeInMillis(timestamp);
		String msg = String.format("[%1$tY.%1$tm.%1$td %1$tH:%1$tM:%1$tS.%1$tL] %2$s\n", c, message); 
		LOG.debug(msg);
		BufferedWriter br = new BufferedWriter(new FileWriter(log, true));
		br.write(msg);
		br.close();
	}
	
	private void zip(File out, File... files) throws IOException {
		byte[] buf = new byte[1024];
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
		//
		for (File filename : files) {
			FileInputStream fis = new FileInputStream(filename);
			zos.putNextEntry(new ZipEntry(filename.getName()));
			int len;
			while ((len = fis.read(buf)) > 0)
				zos.write(buf, 0, len);
			zos.closeEntry();
		}
		zos.close();
	}

	// ====================================
	// ICubeManageDAO
	// ====================================
	public File listVMs(X509Certificate x509) {
		lock.lock();
		try {
			log(x509, "(WebService) listVMs");
			//
			String dn = x509.getSubjectDN().getName();
			String dnHash = HashUtil.sha512UrlInBase64(dn);
			// list files in user directory
			File userDir = new File(baseDir, dnHash);
			ArrayList<File> fileToSend = new ArrayList<File>();
			LOG.debug("List files from ["+userDir.getAbsolutePath()+"]..");
			for (File file : userDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".key.cube") || name.endsWith(".desc.cube");
				}
			})) {
				LOG.debug("append file ["+file.getAbsolutePath()+"]");
				fileToSend.add(file);				
			}
			// zip files
			File tmp = File.createTempFile("CubeManagerDAO", ".data");
			tmp.deleteOnExit();
			zip(tmp, fileToSend.toArray(new File[fileToSend.size()]));
			return tmp;
			// log into user directory
		} catch (Exception e) {
			throw new RuntimeException("Failed to list user VMs.",e);
		} finally {
			lock.unlock();
		}
	}

	public void storePublicKey(java.security.cert.X509Certificate x509) {
		lock.lock();
		try {
			//
			String dn = x509.getSubjectDN().getName();
			String dnHash = HashUtil.sha512UrlInBase64(dn);
			// check/create user directory
			File userDir = new File(baseDir, dnHash);
			if (!userDir.exists()) {
				userDir.mkdirs();
				// create a file with user DN in clear text inside. Ony for
				// debug purpose, may be removed.
				File userCfg = new File(userDir, "user_config");
				FileWriter fw = new FileWriter(userCfg);
				fw.write("DN=" + dn + "\nDN_hash=" + dnHash + "\n");
				fw.close();
			}
			//
			File pemFile = new File(userDir, dnHash + ".pem");
			if (pemFile.exists()) {
				// load and compare to check for modifications
				X509Certificate oldX509 = readPemFile(pemFile);
				if (!Arrays.equals(oldX509.getEncoded(), x509.getEncoded())) {
					LOG.debug("Rename old PEM and save new one [{}].", pemFile.getAbsolutePath());
					// save old certificate under another name
					writePemFile(new File(userDir, String.format("%s-%2$tY%2$tm%2$td.pem", dnHash, new Date())), oldX509);
					// save new certificate
					writePemFile(pemFile, x509);
				}
			} else {
				LOG.debug("Save new certificate [{}].", pemFile.getAbsolutePath());
				// write pem file
				writePemFile(pemFile, x509);
			}
		} catch (Exception e) {
			LOG.error("Failed to store public key.", e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void report(X509Certificate x509, String message, long timestamp) {
		lock.lock();
		try {
			log(x509, message, timestamp);
		} catch (Exception e) {
			LOG.error("Failed to report message [" + message + "].", e);
		} finally {
			lock.unlock();
		}
	}
}
