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

package ch.admin.vbs.cube.client.wm.xrandx.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.State;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.XRResolution;
import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;

public class XrandrCLI implements IXrandr {
	private static final Logger LOG = LoggerFactory.getLogger(XrandrCLI.class);
	private ShellUtil su = new ShellUtil();
	private HashMap<String, XRScreen> screenCache = new HashMap<String, XRScreen>();
	private Object lock = new Object();

	@Override
	public void start() {
		reloadConfiguration();
	}

	@Override
	public List<XRScreen> getScreens() {
		synchronized (lock) {
			return new ArrayList<XRScreen>(screenCache.values());
		}
	}


	private String query() throws ShellUtilException {
		synchronized (lock) {
			su.run(null, ShellUtil.NO_TIMEOUT, "xrandr", "-q");
			return su.getStandardOutput().toString();
		}
	}

	@Override
	public void reloadConfiguration() {
		try {
			// list and parse screens info (using xrandx and regular
			// expressions)
			ArrayList<XRScreen> screens = new ArrayList<XRScreen>();
			String[] lines = query().split("\\n");
			Pattern monLine = Pattern.compile("^(\\w+) (\\w+)( (\\d+)x(\\d+)\\+(\\d+)\\+(\\d+))? .*");
			Pattern resLine = Pattern.compile("^\\s+(\\d+)x(\\d+)\\s+(.*)$");
			Pattern freqPat = Pattern.compile("([\\d\\.]+)(\\*?)");
			ArrayList<XRResolution> resolutions = null;
			String screenId = null;
			State screenState = null;
			String selectedFreq = null;
			XRResolution selectedRes = null;
			int posx = 0, posy = 0;
			RESLOOP: for (int i = 0; i < lines.length; i++) {
				// lookup for resolution line
				if (screenId != null) {
					Matcher m = resLine.matcher(lines[i]);
					if (m.matches()) {
						int width = Integer.parseInt(m.group(1));
						int height = Integer.parseInt(m.group(2));
						ArrayList<String> freqs = new ArrayList<String>();
						Matcher fm = freqPat.matcher(m.group(3));
						boolean selected = false;
						while (fm.find()) {
							freqs.add(fm.group(1));
							if (fm.group(2).length() > 0) {
								selectedFreq = fm.group(1);
								selected = true;
							}
						}
						// create XRResolution object
						XRResolution res = new XRResolution(width, height, freqs);
						if (selected) {
							selectedRes = res;
						}
						resolutions.add(res);
						if (i + 1 == lines.length) {
							// last line
							// create and store XRScreen object
							XRScreen screen = new XRScreen(screenId, screenState, posx, posy, resolutions, selectedRes, selectedFreq);
							screens.add(screen);
							continue RESLOOP;
						} else {
							// next line
							continue RESLOOP;
						}
					} else {
						// create and store XRScreen object
						XRScreen screen = new XRScreen(screenId, screenState, posx, posy, resolutions, selectedRes, selectedFreq);
						screens.add(screen);
						// proceed line as a monitor line (see below)
						resolutions = null;
						screenId = null;
						screenState = State.DISCONNECTED;
						posx = 0;
						posy = 0;
						selectedRes = null;
						selectedFreq = null;
					}
				}
				// lookup for monitor line
				if (screenId == null) {
					Matcher m = monLine.matcher(lines[i]);
					if (m.matches()) {
						resolutions = new ArrayList<XRScreen.XRResolution>();
						screenId = m.group(1);
						if ("connected".equals(m.group(2))) {
							if (m.group(6) == null) {
								screenState = State.CONNECTED;
								posx = 0;
								posy = 0;
							} else {
								screenState = State.CONNECTED_AND_ACTIVE;
								posx = Integer.parseInt(m.group(6));
								posy = Integer.parseInt(m.group(7));
							}
						} else {
							screenState = State.DISCONNECTED;
						}
					}
				}
			}
			LOG.debug("[{}] screens found.", screens.size());
			// check for new/removed/modified screens
			synchronized (screenCache) {
				@SuppressWarnings("unchecked")
				HashMap<String, XRScreen> screenCacheCopy = (HashMap<String, XRScreen>) screenCache.clone();
				for (XRScreen s : screens) {
					XRScreen c = screenCacheCopy.remove(s.getId());
					if (c == null) {
						// New screen. Create and keep a reference in cache.
						LOG.debug("New screen found [{}] .", s.getId());
						screenCache.put(s.getId(), s);
					} else {
						// Known screen. Update cache reference
						screenCache.put(s.getId(), s);
					}
				}
				for (XRScreen c : screenCacheCopy.values()) {
					// Removed screens. Remove reference from cache.
					LOG.debug("Screen [{}] has been removed.", c.getId());
					screenCache.remove(c.getId());
				}
			}
//			// notify listener that the cache has been refreshed
//			fireChanges();
		} catch (ShellUtilException e) {
			LOG.error("Failed to call xrandr", e);
		}
	}

	public void setScreen(XRScreen screen, boolean active, int xpos, int ypos) {
		synchronized (lock) {
			try {
				su.run(null, ShellUtil.NO_TIMEOUT, "xrandr", "--output", screen.getId(), "--pos", xpos + "x" + ypos, active ? "--auto" : "--off");
				Thread.sleep(300);
				if (active) {
					// ensure that lcd is not tuned off.
					su.run(null, ShellUtil.NO_TIMEOUT, "xset", "dpms", "force", "on");
				}
			} catch (Exception e) {
				LOG.error("Failed to set screen position wit xrandr", e);
				LOG.error(su.getStandardError().toString());
			}
		}
	}

}
