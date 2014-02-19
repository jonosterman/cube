package net.cube.filesafe;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import net.cube.common.CubeConfig;
import net.cube.common.CubeException;
import net.cube.common.crypto.AESEncrypter;
import net.cube.common.crypto.HashUtil;
import net.cube.common.crypto.RSAEncryptUtil;
import net.cube.common.shell.ShellUtil;
import net.cube.common.shell.ShellUtilException;
import net.cube.token.IIdentityToken;
import net.cube.token.IIdentityToken.KeyType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSafe {
	private static final Logger LOG = LoggerFactory.getLogger(FileSafe.class);
	private File safeDir;
	private IIdentityToken idt;

	public FileSafe() {
	}

	public void setup(File safeDir, IIdentityToken idt) {
		this.safeDir = safeDir;
		this.idt = idt;
		safeDir.mkdirs();
	}

	public void open() {
		// nothing yet
	}

	public void close() {
		// nothing yet
	}

	/**
	 * move file to safe (encrypt it) and shred the original file
	 * 
	 * @throws IOException
	 * @throws CubeException
	 */
	public File moveToSafe(File inFile, String id) throws IOException, CubeException {
		// use id's hash as filename in safe
		String hId = HashUtil.sha256UrlInBase64(id);
		File outFile = new File(safeDir, hId);
		// generate symmetric key to encrypt this file
		SecretKey sec = AESEncrypter.generateKey(CubeConfig.getPropertyAsInt("safe.aeskeylength", 256));
		AESEncrypter enc = new AESEncrypter(sec);
		FileOutputStream fos = new FileOutputStream(outFile);
		FileInputStream fis = new FileInputStream(inFile);
		// encrypt symmetric key
		byte[] encSym = RSAEncryptUtil.encrypt(sec.getEncoded(), idt.getPublickey(KeyType.ENCIPHERMENT));
		// write symmetric key length and key itself
		fos.write(encSym.length >> 8 & 0xff);
		fos.write(encSym.length & 0xff);
		fos.write(encSym);
		// encrypt inFile content
		enc.encrypt(fis, fos);
		fis.close();
		fos.close();
		// shred original file
		shred(inFile);
		return outFile;
	}

	public void copyFromSafe(String id, File outFile) throws IOException, CubeException {
		String hId = HashUtil.sha256UrlInBase64(id);
		File inFile = new File(safeDir, hId);
		// read encrypted symmetric key length
		BufferedInputStream fis = new BufferedInputStream(new FileInputStream(inFile));
		BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(outFile));
		int kLen = fis.read() & 0xff;
		kLen = kLen << 8 | (fis.read() & 0xff);
		byte[] encKey = new byte[kLen];
		fis.read(encKey);
		// decipher symmetrical key
		byte[] decSym = RSAEncryptUtil.decrypt(encKey, idt.getPrivatekey(KeyType.ENCIPHERMENT));
		SecretKey sec = new SecretKeySpec(decSym, "AES");
		AESEncrypter enc = new AESEncrypter(sec);
		enc.decrypt(fis, fos);
		//
		fis.close();
		fos.close();
	}

	public void shred(File file) {
		ShellUtil su = new ShellUtil();
		try {
			su.run("shred", "-u", file.getAbsolutePath());
		} catch (ShellUtilException e) {
			LOG.error("Failed to shred input file [" + file.getAbsolutePath() + "]");
			if (!file.delete()) {
				LOG.error("Failed to delete input file [" + file.getAbsolutePath() + "]");
			}
		}
	}
}
