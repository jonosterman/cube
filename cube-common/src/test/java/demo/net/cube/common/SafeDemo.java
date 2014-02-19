package demo.net.cube.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.KeyStoreException;

import net.cube.common.CubeException;
import net.cube.filesafe.FileSafe;
import net.cube.token.IdentityToken;
import net.cube.token.PwdCallbackHandler;

/**
 * Access sample configuration values from XML
 */
public class SafeDemo {
	public SafeDemo() {
	}

	public static void main(String[] args) throws Exception {
		SafeDemo d = new SafeDemo();
		d.start();
	}

	private void start() throws IOException, KeyStoreException, CubeException {
		File jksFile = new File(System.getProperty("user.home") + "/cube-pki/client1.jks");
		final char[] pwd = "123456".toCharArray();
		Builder builder = KeyStore.Builder.newInstance("JKS", null, jksFile, new KeyStore.CallbackHandlerProtection(new PwdCallbackHandler(pwd)));
		KeyStore ks = builder.getKeyStore();
		IdentityToken idt = new IdentityToken(ks, builder, pwd);
		//
		FileSafe safe = new FileSafe();
		File dir = new File("/tmp/test-safe");
		safe.setup(dir, idt);
		// create file and move it into safe
		File x = writeFile("blahblablahxxx");
		safe.moveToSafe(x, "x");
		System.out.println("File moved into safe");
		// retrieve it
		File y = File.createTempFile("test-clear_", ".txt");
		y.deleteOnExit();
		safe.copyFromSafe("x", y);
		System.out.println("Decrypted file content: " + readFile(y));
		safe.shred(y);
		//
		System.out.println("done.");
	}

	private String readFile(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuilder sb = new StringBuilder();
		char[] in = new char[64];
		int x = br.read(in);
		while (x > 0) {
			sb.append(in, 0, x);
			x = br.read(in);
		}
		br.close();
		return sb.toString();
	}

	private File writeFile(String content) throws IOException {
		File x = File.createTempFile("test_", ".data");
		x.deleteOnExit();
		BufferedWriter be = new BufferedWriter(new FileWriter(x));
		be.write(content);
		be.close();
		return x;
	}
}
