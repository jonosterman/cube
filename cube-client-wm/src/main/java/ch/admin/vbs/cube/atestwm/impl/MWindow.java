package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Rectangle;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

/**
 * Managed windows.
 * 
 * in X environment windows position is outside the border but the window's
 * width and height are measured without the borders. The window's origin (where
 * the content take place, and from where the children windows' position are
 * computed) is within the borders to.
 * 
 * Do not confused with java that use window's bound as a rectangle containing
 * the windows (including borders). Such bound has no equivalent in X.
 * 
 */
public class MWindow {
	public static final int BORDER_WM = 2;
	public static final int BORDER_DEB = 5;
	private Rectangle eBounds;
	private final Window xwindow;
	private Window xclient;
	private final int border;
	private Rectangle xBounds;

	public MWindow(Window xwindow, Rectangle externalBounds, int border) {
		this.xwindow = xwindow;
		this.eBounds = externalBounds;
		this.border = border;
		updateXBounds();
	}

	// ###############################
	private void updateXBounds() {
		// xBounds keep window ORIGIN and windows width and height according X
		// meaning. window POSITION is kept by eBounds.
		xBounds = new Rectangle(eBounds.x + border, eBounds.y + border, eBounds.width - 2 * border, eBounds.height - 2 * border);
	}

	// ###############################
	public Rectangle getExternBounds() {
		return eBounds;
	}

	public Rectangle getXBounds() {
		return xBounds;
	}

	public void setBounds(Rectangle bounds) {
		this.eBounds = bounds;
		updateXBounds();
	}

	public Window getXWindow() {
		return xwindow;
	}

	public Window getXClient() {
		return xclient;
	}

	public void setXclient(Window xclient) {
		this.xclient = xclient;
	}
}
