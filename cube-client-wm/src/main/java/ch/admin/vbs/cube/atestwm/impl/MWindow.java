package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Rectangle;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public class MWindow {
	private Rectangle bounds;
	private final Window xwindow;
	private Window xclient;
	private final int border;
	private Rectangle clientBounds;

	public MWindow(Window xwindow, Rectangle bounds, int border) {
		this.xwindow = xwindow;
		this.bounds = bounds;
		this.border = border;
		updateClientBounds();
	}

	// ###############################
	private void updateClientBounds() {
		clientBounds = new Rectangle(bounds.x + border, bounds.y + border, bounds.width - 2 * border, bounds.height - 2 * border);
	}

	// ###############################
	public Rectangle getBounds() {
		return bounds;
	}

	public Rectangle getClientBounds() {
//		return clientBounds;
		return bounds;
	}

	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
		updateClientBounds();
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
