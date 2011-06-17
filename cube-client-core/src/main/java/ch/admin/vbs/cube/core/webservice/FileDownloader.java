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

package ch.admin.vbs.cube.core.webservice;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.container.SizeFormatUtil;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;

public class FileDownloader implements Runnable {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(FileDownloader.class);

	public enum State {
		IDLE, DOWNLOADING, SUCCESS, FAILED
	}

	private State state = State.IDLE;
	private Thread thread;
	private String server;
	private int port;
	private String uuid;
	private long size;
	private long received;
	private OutputStream dstStream;
	private Builder builder;

	public FileDownloader(Builder builder) {
		this.builder = builder;
	}

	public State getState() {
		return state;
	}

	public void setRequest(String server, int port, String uuid, long size) {
		this.server = server;
		this.port = port;
		this.uuid = uuid;
		this.size = size;
	}

	public void setRequest(String uuid, long size) {
		this.server = CubeClientCoreProperties.getProperty("webservice.cubemanager.host");
		this.port = Integer.parseInt(CubeClientCoreProperties.getProperty("webservice.cubemanager.port"));
		this.uuid = uuid;
		this.size = size;
	}

	public void setDestination(OutputStream dstStream) {
		this.dstStream = dstStream;
	}

	public void startDownload() {
		thread = new Thread(this, "Downloader-XX");
		state = State.DOWNLOADING;
		thread.start();
	}

	public void run() {
		Socket sk = null;
		try {
			LOG.debug("Entering download thread [{}]", SizeFormatUtil.format(size));
			// open socket and start download
			String proto = CubeClientCoreProperties.getProperty("webservice.cubemanager.protocol");
			if ("https".equalsIgnoreCase(proto)) {
				KeyStore trustStore = KeyStore.getInstance("jks");
				File truststore = new File(CubeClientCoreProperties.getProperty("rootca.keystore.file"));
				FileInputStream fis = new FileInputStream(truststore);
				trustStore.load(fis, CubeClientCoreProperties.getProperty("rootca.keystore.password").toCharArray());
				SSLSocketFactory slf = CubeSSLSocketFactory.newSSLSocketFactory(builder, trustStore, false);
				sk = slf.createSocket(server, port);
				fis.close();
			} else {
				sk = new Socket(server, port);
			}
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sk.getOutputStream()));
			String request = "GET " + CubeClientCoreProperties.getProperty("webservice.cubemanager.downloadUri") + uuid + " HTTP/1.0";
			LOG.debug("Send request '{}'", request);
			bw.write(request + "\n\n");
			bw.flush();
			BufferedInputStream bi = new BufferedInputStream(sk.getInputStream());
			// consume HTTP header
			int h = bi.read() & 0xff;
			int owf = 0;
			int crnl = 0;
			StringBuffer header = new StringBuffer();
			while (owf++ < 1000) {
				header.append((char) h);
				if (h == 10 || h == 13) {
					if (++crnl > 3) {
						break;
					}
				} else {
					crnl = 0;
				}
				h = bi.read() & 0xff;
			}
			LOG.debug("Consumed header [{}]", owf);
			Pattern sizeRegex = Pattern.compile("Content-Length: (\\d+)");
			Matcher m = sizeRegex.matcher(header.toString());
			if (m.find()) {
				String size = m.group(1);
				LOG.debug("Size found in header [{}]", size);
				try {
					this.size = Long.parseLong(size);
				} catch (Exception e) {
					LOG.error("Failed to parse size found in header [{}]", size);
					LOG.error("Response header: ");
					LOG.error(header.toString());
					LOG.error("Server/port [{}] [{}]", server, port);
					LOG.error("Failure for reque0st: {}", request);
				}
			} else {
				LOG.error("No Size found in header [{}]", header);
			}
			// download file
			received = 0;
			byte[] buffer = new byte[1024 * 100];
			int cnt = bi.read(buffer);
			BufferedOutputStream bo = new BufferedOutputStream(dstStream);
			long last = 0;
			while (cnt > 0) {
				received += cnt;
				if ((received / 1048576000) != last) {
					last = received / 1048576000;
					LOG.debug("Download progress [{}]", SizeFormatUtil.format(received));
				}
				bo.write(buffer, 0, cnt);
				cnt = bi.read(buffer);
			}
			bo.flush();
		} catch (Exception e) {
			LOG.error("Failed to download file", e);
			state = State.FAILED;
			return;
		} finally {
			try {
				sk.close();
			} catch (IOException e) {
				LOG.error("Download failed.", e);
			}
			LOG.debug("Got bytes [{}/{}]", received, size);
		}
		state = State.SUCCESS;
	}

	public double getProgress() {
		return received / (double) size;
	}
}
