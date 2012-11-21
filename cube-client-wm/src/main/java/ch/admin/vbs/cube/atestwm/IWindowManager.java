package ch.admin.vbs.cube.atestwm;

import java.awt.Rectangle;

import ch.admin.vbs.cube.atestwm.impl.MWindow;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public interface IWindowManager {

	MWindow createAndMapWindow(Rectangle externBnds, int border);

	void disposeWindow(MWindow window);

	void moveAndResizeWindow(MWindow bgWindow, Rectangle newBnds);
}
