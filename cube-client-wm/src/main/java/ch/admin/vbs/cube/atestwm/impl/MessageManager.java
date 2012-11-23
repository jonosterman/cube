package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.IMessageManager;
import ch.admin.vbs.cube3.core.ui.MessageFrame;

public class MessageManager implements IMessageManager {
	private static final Logger LOG = LoggerFactory.getLogger(MessageManager.class);
	private HashMap<String, MessageFrame> panels = new HashMap<String, MessageFrame>();

	public void setup() {
	}

	@Override
	public MessageFrame createTabPanel(String fId, Rectangle bounds) {
		LOG.debug("Create Message Frame [{}]", fId);
		MessageFrame frame = new MessageFrame(fId);
		panels.put(fId, frame);
		frame.pack();
		frame.setVisible(true);
		return frame;
	}

	@Override
	public void disposeTabPanel(String fId) {
		MessageFrame tf = panels.get(fId);
		tf.setVisible(false);
		tf.dispose();
	}

	@Override
	public void updateTabPanel(String fId, Rectangle bounds) {
		MessageFrame tf = panels.get(fId);
		tf.setPreferredSize(new Dimension(bounds.width, bounds.height));
		tf.pack();
	}

	@Override
	public boolean matchMsgPanel(String winName) {
		return panels.containsKey(winName);
	}
}
