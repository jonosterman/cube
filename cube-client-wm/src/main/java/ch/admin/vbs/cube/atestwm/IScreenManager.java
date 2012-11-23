package ch.admin.vbs.cube.atestwm;

import ch.admin.vbs.cube.atestwm.impl.MWindow;
import ch.admin.vbs.cube.atestwm.impl.ScreenManager.Screen;
import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public interface IScreenManager {

	MWindow getTabOrMsgWindow(String winName);

	MWindow getAppWindow(Window window);
	
	Screen getDefaultScreen();

}
