package net.cube.common.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import sun.security.provider.X509Factory;

/**
 * Write certificate in files. use ssl to check them : 'openssl x509 -text -in you-certificate.pem'
 */
public class PemToolkit {
	public static void writePem(X509Certificate x509, File file) throws IOException, CertificateEncodingException {
		FileWriter fw = new FileWriter(file);
		fw.write(X509Factory.BEGIN_CERT + '\n');
		fw.write(split(Base64.encodeBytes(x509.getEncoded()),64));
		fw.write('\n' + X509Factory.END_CERT + '\n');
		fw.close();
	}

	private static String split(String string, int wide) {
		int i = 0;
		StringBuffer buffer = new StringBuffer();
		while (i < string.length()) {
			buffer.append(string.subSequence(i, Math.min(i+wide, string.length())));			
			i += wide;
			if (i < string.length()) {
				buffer.append('\n');
			}
		}
		return buffer.toString();
	}

	public static X509Certificate readPem(File pemFile) throws CertificateException, FileNotFoundException {
		// decode
		CertificateFactory fact = CertificateFactory.getInstance("X.509");
		return (X509Certificate) fact.generateCertificate(new FileInputStream(pemFile));
	}
}
