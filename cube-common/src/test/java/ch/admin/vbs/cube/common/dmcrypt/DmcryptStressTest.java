
package ch.admin.vbs.cube.common.dmcrypt;

import java.io.File;
import java.io.FileWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyStore.Builder;
import java.util.ArrayList;
import java.util.Random;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.ContainerException;
import ch.admin.vbs.cube.common.container.impl.DmcryptContainerFactory;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;

public class DmcryptStressTest {
	private static final String testKeystorePassword = "111222";
	private static final String testKeystoreFile = "/cube-01_pwd-is-111222.p12";
	private static DmcryptContainerFactory factory;
	private Random rnd = new Random(System.currentTimeMillis());

	public static void main(String[] args) throws Exception {
		DmcryptStressTest d = new DmcryptStressTest();
		factory = new DmcryptContainerFactory();
		DmcryptContainerFactory.cleanup();
		new File("/tmp/dmcrypt/containers/").mkdirs();
		new File("/tmp/dmcrypt/mp/").mkdirs();
		// key stuff
		File key = new File("/tmp/dmcrypt/key");
		if (!key.exists()) {
			FileWriter fw = new FileWriter(key);
			fw.write("Very-very-secret-key!");
			fw.close();
		}
		EncryptionKey ekey = new EncryptionKey("mykey", key);
		// create fake containers
		ArrayList<Container> cnts = new ArrayList<Container>();
		for (int i = 0; i < 10; i++) {
			cnts.add(genContainer("containe-" + i));
			if (!cnts.get(i).getContainerFile().exists()) {
				factory.createContainer(cnts.get(i), ekey);
			}
		}
		d.start(cnts, ekey);
	}

	private static Container genContainer(String id) {
		Container c = new Container();
		c.setContainerFile(new File("/tmp/dmcrypt/containers/" + id));
		if (c.getContainerFile().exists()) {
			c.getContainerFile().delete();
		}
		c.setId(id);
		c.setMountpoint(new File("/tmp/dmcrypt/mp/" + id));
		c.setSize(10000000);
		return c;
	}

	private void start(ArrayList<Container> cnts, EncryptionKey key) throws Exception {
		for (Container c : cnts) {
			factory.mountContainer(c, key);
		}
		for (Container c : cnts) {
			factory.unmountContainer(c);
		}
	}
}
