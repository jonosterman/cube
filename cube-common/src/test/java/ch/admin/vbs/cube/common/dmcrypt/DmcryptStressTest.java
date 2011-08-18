/**
 * Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ch.admin.vbs.cube.common.dmcrypt;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.impl.DmcryptContainerFactory;
import ch.admin.vbs.cube.common.keyring.EncryptionKey;

/**
 * We wrote this class in order to test container mounting/un-mounting since we
 * observer bug in production (container left mounted, corrupted lock file,
 * etc). It helped to improve overall code quality and error handling (java +
 * perl).
 * 
 */
public class DmcryptStressTest {
	private static DmcryptContainerFactory factory;

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
