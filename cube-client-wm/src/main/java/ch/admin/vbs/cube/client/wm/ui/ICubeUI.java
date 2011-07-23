package ch.admin.vbs.cube.client.wm.ui;

import java.util.List;

import javax.swing.JFrame;

import ch.admin.vbs.cube.client.wm.ui.CubeUI.CubeScreen;
import ch.admin.vbs.cube.client.wm.xrandx.impl.XrandrTwoDisplayLayout.Layout;

public interface ICubeUI {

	CubeScreen getDefaultScreen();

	List<CubeScreen> getScreens();

	CubeScreen getScreen(String monitorId);

	void layoutScreens(Layout layout);
	
}
