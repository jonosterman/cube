package ch.admin.vbs.cube.atestwm;

import java.awt.Rectangle;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public interface IScreenManager {
	Window getTabWindow(String winName);

	Rectangle getTabWindowBounds(String winName);
}
