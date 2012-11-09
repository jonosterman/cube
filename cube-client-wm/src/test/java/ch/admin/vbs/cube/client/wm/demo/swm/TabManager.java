package ch.admin.vbs.cube.client.wm.demo.swm;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.demo.swm.IMonitorLayout.IMonitorLayoutListener;
import ch.admin.vbs.cube.client.wm.xrandx.IXrandr;
import ch.admin.vbs.cube.client.wm.xrandx.XRScreen;

public class TabManager {
	private static final Logger LOG = LoggerFactory.getLogger(TabManager.class);
	public static final int TAB_BAR_HEIGHT = 25;
	private HashMap<String, JFrame> tabPanels = new HashMap<String, JFrame>();
	private IXrandr xrandr;

	public void setup(IMonitorLayout layout, IXrandr xrandr) {
		this.xrandr = xrandr;
		layout.addListener(new MonitorLayoutHandler());
	}

	public class MonitorLayoutHandler implements IMonitorLayoutListener {
		@Override
		public void layoutChanged() {
			HashSet<JFrame> aPanels = new HashSet<JFrame>();
			for (XRScreen s : xrandr.getScreens()) {
				// make sure that a bg frame is define for each screen
				JFrame frame = tabPanels.get(s.getId());
				if (frame == null) {
					LOG.debug("Create tab panel for screen {}", s.getId());
					// create new tab panel
					frame = new JFrame(s.getId());
					frame.getContentPane().setPreferredSize(new Dimension(s.getCurrentWidth(), TAB_BAR_HEIGHT));
					tabPanels.put(s.getId(), frame);
					frame.pack();
					frame.setVisible(true);
					aPanels.add(frame);
				} else {
					LOG.debug("Update tab panel for screen {}", s.getId());
					frame.getContentPane().setPreferredSize(new Dimension(s.getCurrentWidth(), TAB_BAR_HEIGHT));
					frame.pack();
					aPanels.add(frame);
				}
			}
			// remove unused bg windows
			ArrayList<JFrame> old = new ArrayList<JFrame>(tabPanels.values());
			System.out.printf("there was %d tabpanels\n", old.size());
			System.out.printf("there is %d tabpanels still in use.\n", aPanels.size());
			old.removeAll(aPanels);
			System.out.printf("there is %d tabpanels to remove.\n", old.size());
			for (JFrame m : old) {
				LOG.debug("Remove {}", m.getTitle());
				// TODO: re-parent window in deleted bg window if any.
				tabPanels.remove(m.getTitle());
			}
		}
	}
}
