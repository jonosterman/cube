package ch.admin.vbs.cube.client.wm.xrandx.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.xrandx.IXRListener;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen.XRResolution;
import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;

public class XrandrCLI implements IXrandr, Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(XrandrCLI.class);
	private ShellUtil su = new ShellUtil();
	private boolean running;
	private HashMap<String, XRScreen> screenCache = new HashMap<String, XRScreen>();
	private ArrayList<IXRListener> listeners = new ArrayList<IXRListener>();
	private Object lock = new Object();

	@Override
	public void start() {
		Thread t = new Thread(this, "XRandxCLI");
		t.start();
		while (screenCache.size() == 0) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			monitorXrandr();
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
			}
		}
	}

	@Override
	public List<XRScreen> getScreens() {
		synchronized (lock) {
			return new ArrayList<XRScreen>(screenCache.values());
		}
	}

	@Override
	public void addListener(IXRListener l) {
		synchronized (lock) {
			listeners.add(l);
		}
	}

	@Override
	public void removeListener(IXRListener l) {
		synchronized (lock) {
			listeners.remove(l);
		}
	}

	private String query() throws ShellUtilException {
		synchronized (lock) {
			su.run(null, ShellUtil.NO_TIMEOUT, "xrandr", "-q");
			return su.getStandardOutput().toString();
		}
	}

	private void monitorXrandr() {
		try {
			// list and parse screens info (using xrandx and regular
			// expressions)
			ArrayList<XRScreen> screens = new ArrayList<XRScreen>();
			LOG.debug("Query xrandr..");
			String[] lines = query().split("\\n");
			Pattern monLine = Pattern.compile("^(\\w+) (\\w+) (\\d+)x(\\d+)\\+(\\d+)\\+(\\d+) \\d+mm x \\d+mm$");
			Pattern resLine = Pattern.compile("^\\s+(\\d+)x(\\d+)\\s+(.*)$");
			Pattern freqPat = Pattern.compile("([\\d\\.]+)(\\*?)");
			ArrayList<XRResolution> resolutions = null;
			String screenId = null;
			String screenState = null;
			String selectedFreq = null;
			XRResolution selectedRes = null;
			int posx = 0, posy = 0;
			RESLOOP: for (int i = 0; i < lines.length; i++) {
				// lookup for resolution line
				if (screenId != null) {
					Matcher m = resLine.matcher(lines[i]);
					if (m.matches()) {
						int height = Integer.parseInt(m.group(1));
						int width = Integer.parseInt(m.group(2));
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
					}
				}
				// lookup for monitor line
				if (screenId == null) {
					Matcher m = monLine.matcher(lines[i]);
					if (m.matches()) {
						resolutions = new ArrayList<XRScreen.XRResolution>();
						screenId = m.group(1);
						screenState = m.group(2);
						posx = Integer.parseInt(m.group(5));
						posy = Integer.parseInt(m.group(6));
					}
				}
			}
			// check for new/removed/modified screens
			int changes = 0;
			synchronized (screenCache) {
				HashMap<String, XRScreen> screenCacheCopy = (HashMap<String, XRScreen>) screenCache.clone();
				System.out.println("" + screens.size());
				for (XRScreen s : screens) {
					XRScreen c = screenCacheCopy.remove(s.getId());
					if (c != null && s.getState().equals(c.getState())) {
						// already known screen
					} else {
						// new or modified screen
						changes++;
						LOG.debug("Screen [{}] has been modified.", s.getId());
						screenCache.put(s.getId(), s);
					}
				}
				for (XRScreen c : screenCacheCopy.values()) {
					// removed screens
					changes++;
					LOG.debug("Screen [{}] has been removed.", c.getId());
					screenCache.remove(c.getId());
				}
			}
			if (changes > 0) {
				LOG.debug("Screen configuration changed");
				fireChanges();
			}
		} catch (ShellUtilException e) {
			LOG.error("Failed to call xrandr", e);
		}
	}

	public void setScreen(XRScreen screen, boolean active, int xpos, int ypos) {
		synchronized (lock) {
			try {
				su.run(null, ShellUtil.NO_TIMEOUT, "xrandr", "--output", screen.getId(), "--pos", xpos + "x" + ypos, active ? "--auto" : "--off");
			} catch (ShellUtilException e) {
				LOG.error("Failed to set screen position wit xrandr", e);
				LOG.error(su.getStandardError().toString());
			}
		}
	}

	private void fireChanges() {
		for (IXRListener l : listeners) {
			l.screenChanged();
		}
	}
}
