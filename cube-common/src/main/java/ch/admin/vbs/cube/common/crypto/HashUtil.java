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

package ch.admin.vbs.cube.common.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class used to hash strings. Hash are used in cube as filenames.
 */
public class HashUtil {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(HashUtil.class);

	/** hash a string and return an URL compatible string. */
	public static String sha256UrlInBase64(String str) {
		if (str == null) {
			return null;
		}
		try {
			return Base64.encodeBytes(new Sha4J().sha256Digest(new ByteArrayInputStream(str.getBytes())), Base64.URL_SAFE);
		} catch (IOException e) {
			LOG.error("Failed to hash [" + str + "].", e);
			return null;
		}
	}

	/** hash a string and return an URL compatible string. */
	public static String sha512UrlInBase64(String str) {
		if (str == null) {
			return null;
		}
		try {
			// replace also trailing '=' with 'X' to be command line friendly
			return Base64.encodeBytes(new Sha4J().sha512Digest(new ByteArrayInputStream(str.getBytes())), Base64.URL_SAFE).replaceAll("=", "X");
		} catch (IOException e) {
			LOG.error("Failed to hash [" + str + "].", e);
			return null;
		}
	}

	public static String md5(String str) {
		MessageDigest digest;
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(str.getBytes("UTF-8"));
			byte[] hash = digest.digest();
			return String.format("%0" + (hash.length << 1) + "X", new BigInteger(1, hash));
		} catch (Exception e) {
			LOG.error("Failed to generate MD5 hash", e);
		}
		return null;
	}
}
