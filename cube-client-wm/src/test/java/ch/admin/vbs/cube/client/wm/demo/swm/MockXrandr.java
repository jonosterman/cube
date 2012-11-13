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
package ch.admin.vbs.cube.client.wm.demo.swm;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.XRResolution;

public class MockXrandr implements IXrandr {
	private static final Logger LOG = LoggerFactory.getLogger(MockXrandr.class);
	private ArrayList<Monitor> monListModel = new ArrayList<MockXrandr.Monitor>();
	private HashMap<String, Monitor> monitors = new HashMap<String, MockXrandr.Monitor>();
	private ArrayList<XRScreen> list;
	private Random rnd = new Random();

	public static void main(String[] args) throws Exception {
		new MockXrandr();
	}

	public MockXrandr() throws Exception {
		monListModel.add(new Monitor("mon#" + monListModel.size(), new Rectangle(0, 0, 320, 200)));
		monListModel.add(new Monitor("mon#" + monListModel.size(), new Rectangle(0, 0, 400, 260)));
		reloadConfiguration();
		//
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ServerSocket serv = new ServerSocket(9122);
					while (true) {
						Socket s = serv.accept();
						try {
							InputStream in = s.getInputStream();
							int c = 0;
							while (c >= 0) {
								c = in.read();
								switch (c) {
								case 1:
									int rmd = rnd.nextInt(3);
									switch (rmd) {
									case 0:
										monListModel.add(new Monitor("mon#" + monListModel.size(), new Rectangle(0, 0, 320, 200)));
										break;
									case 1:
										monListModel.add(new Monitor("mon#" + monListModel.size(), new Rectangle(0, 0, 240, 150)));
										break;
									case 2:
										monListModel.add(new Monitor("mon#" + monListModel.size(), new Rectangle(0, 0, 160, 100)));
										break;
									}
									break;
								case 2:
									if (monListModel.size() > 1) {
										monListModel.remove(monListModel.size() - 1);
									}
									break;
								default:
									LOG.debug("Bad command [{}]", c);
									break;
								}
								LOG.debug("Screen list updated [{}]", monListModel.size());
							}
						} catch (IOException e) {
							LOG.debug(e.getMessage());
						}
					}
				} catch (Exception e) {
					LOG.error("Failed to start server", e);
				}
			}
		}).start();
	}

	public void start() {
	}

	private class Monitor {
		Rectangle bounds;
		String id;
		public boolean active = true;

		public Monitor(String id, Rectangle bounds) {
			this.id = id;
			this.bounds = bounds;
		}

		@Override
		public String toString() {
			return String.format("%s (%d:%d)(%dx%d)", id, bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}

	@Override
	public List<XRScreen> getScreens() {
		return list;
	}

	@Override
	public void reloadConfiguration() {
		list = new ArrayList<XRScreen>();
		for (int i = 0; i < monListModel.size(); i++) {
			Monitor m = (Monitor) monListModel.get(i);
			List<XRResolution> resolutions = new ArrayList<XRScreen.XRResolution>();
			XRResolution selectedResolution = new XRResolution(m.bounds.width, m.bounds.height, new ArrayList<String>());
			resolutions.add(selectedResolution);
			XRScreen s = new XRScreen(m.id, State.CONNECTED_AND_ACTIVE, m.bounds.x, m.bounds.y, resolutions, selectedResolution, "50");
			list.add(s);
		}
	}

	@Override
	public void setScreen(XRScreen xrScreen, boolean active, int x, int y) {
		for (int i = 0; i < monListModel.size(); i++) {
			Monitor m = (Monitor) monListModel.get(i);
			if (m.id.equals(xrScreen.getId())) {
				m.bounds.x = x;
				m.bounds.y = y;
				m.active = active;
			}
		}
	}
}
