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

package ch.admin.vbs.cube.core.login;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.impl.DmcryptContainerFactory;
import ch.admin.vbs.cube.core.impl.TransferContainerFactory;
import ch.admin.vbs.cube.core.mock.MockIdentityToken;

public class TestTransferFactoryMix {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(TestTransferFactoryMix.class);

	/** create and dispose container randomly */
	@Test
	public void testSerie() {
		try {
			MockIdentityToken ids[] = new MockIdentityToken[5];
			ids[0] = new MockIdentityToken("0000");
			ids[1] = new MockIdentityToken("1111");
			ids[2] = new MockIdentityToken("2222");
			ids[3] = new MockIdentityToken("3333");
			ids[4] = new MockIdentityToken("4444");
			TransferContainerFactory factory = new TransferContainerFactory();
			DmcryptContainerFactory cf = new DmcryptContainerFactory();
			factory.setContainerFactory(cf);
			// create 3 container
			Container cnt0a = factory.initTransfer(ids[0]);
			Container cnt1a = factory.initTransfer(ids[1]);
			Container cnt2a = factory.initTransfer(ids[2]);
			// re-create an openend container
			Container cnt0b = factory.initTransfer(ids[0]);
			// close 2 time a container
			factory.disposeTransfer(cnt0a);
			factory.disposeTransfer(cnt0b);
			// close other
			factory.disposeTransfer(cnt1a);
			factory.disposeTransfer(cnt2a);
		} catch (Exception e) {
			LOG.error("Failure", e);
		}
	}
}
