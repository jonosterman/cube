/**
 * Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package ch.admin.vbs.cube.common;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import ch.admin.vbs.cube.common.crypto.Base64;
import ch.admin.vbs.cube.common.crypto.Sha4J;

/**
 * Test url encoding.
 * 
 * @author dreier
 * 
 */
public class EncodingTest {
	@Test
	public void testEncoding() throws Exception {
		String id = "test name";
		System.out.println("Clear:" + id);
		System.out.println("Base64: " + Base64.encodeBytes(id.getBytes()));
		System.out.println("Base64url: " + Base64.encodeBytes(id.getBytes(), Base64.URL_SAFE));
		System.out.println("Base64url+gzip: " + Base64.encodeBytes(id.getBytes(), Base64.URL_SAFE | Base64.GZIP));
		Sha4J sha = new Sha4J();
		System.out.println("SHA256: " + sha.sha256Digest(new ByteArrayInputStream(id.getBytes())).length);
		System.out.println("SHA512: " + sha.sha512Digest(new ByteArrayInputStream(id.getBytes())).length);
		System.out.println("SHA256+Base64url: " + Base64.encodeBytes(sha.sha256Digest(new ByteArrayInputStream(id.getBytes())), Base64.URL_SAFE));
		System.out.println("SHA512+Base64url: " + Base64.encodeBytes(sha.sha512Digest(new ByteArrayInputStream(id.getBytes())), Base64.URL_SAFE));
		System.out.println("done");
	}

	public static void main(String[] args) throws Exception {
		EncodingTest t = new EncodingTest();
		t.testEncoding();
	}
}
