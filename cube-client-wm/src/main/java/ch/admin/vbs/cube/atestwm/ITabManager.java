package ch.admin.vbs.cube.atestwm;

import java.awt.Rectangle;

import ch.admin.vbs.cube.atestwm.impl.TabFrame;

public interface ITabManager {
	public TabFrame createTabPanel(String frameTitle, Rectangle bounds);
	public void updateTabPanel(String frameTitle, Rectangle bounds);
	public void disposeTabPanel(String frameTitle);
	public boolean matchTabPanel(String winName);
}
