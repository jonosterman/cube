package test.old;

import java.io.File;
import java.net.URL;

/**
 * @see README-test-PKI.txt to see how to generate PKI.
 */
public class TestPKI {
	public static File getPKIDirectory() {
		URL url = TestPKI.class.getResource("/cube-pki");
		return new File(url.getFile());
	}
}
