package ch.admin.vbs.cube.client.wm.xrandx;

import java.util.List;

public interface IXrandr {
	void start();

	void addListener(IXRListener l);

	void removeListener(IXRListener l);

	List<XRScreen> getScreens();

	void setScreen(XRScreen xrScreen, boolean connected, int x, int y);
}
