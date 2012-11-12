package ch.admin.vbs.cube.atestwm;

import java.awt.Rectangle;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public interface IWindowManager {

	Window createAndMapWindow(Rectangle bgBnds);

	void disposeWindow(Window window);

	void moveAndResizeWindow(Window bgWindow, Rectangle bgBnds);
}
