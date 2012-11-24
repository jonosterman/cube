package ch.admin.vbs.cube.atestwm;

import java.awt.Rectangle;

import ch.admin.vbs.cube3.core.ui.MessageFrame;

public interface IMessageManager {
	public MessageFrame createPanel(String frameTitle, Rectangle bounds);
	public void updatePanel(String frameTitle, Rectangle bounds);
	public void disposePanel(String frameTitle);
	public boolean matchPanelName(String winName);
}
