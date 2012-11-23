package ch.admin.vbs.cube.atestwm;

import java.awt.Rectangle;

import ch.admin.vbs.cube3.core.ui.MessageFrame;

public interface IMessageManager {
	public MessageFrame createTabPanel(String frameTitle, Rectangle bounds);
	public void updateTabPanel(String frameTitle, Rectangle bounds);
	public void disposeTabPanel(String frameTitle);
	public boolean matchMsgPanel(String winName);
}
