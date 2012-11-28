package ch.admin.vbs.cube.atestwm;

import java.awt.Rectangle;

import ch.admin.vbs.cube.atestwm.impl.MWindow;

public interface IWindowManager {

	MWindow createAndMapWindow(Rectangle externBnds, int border);

	void disposeWindow(MWindow window);

	void moveAndResizeWindow(MWindow bgWindow, Rectangle newBnds);
}
