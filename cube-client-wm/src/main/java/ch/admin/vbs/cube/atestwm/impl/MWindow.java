package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Rectangle;

import ch.admin.vbs.cube.client.wm.ui.x.imp.X11.Window;

public class MWindow {
	private Rectangle bounds;
	private final Window xwindow;
	private Window xclient;
	
	
	public MWindow(Window xwindow, Rectangle bounds) {
		this.xwindow = xwindow;
		this.bounds = bounds;
	}
	
	// ###############################

	// ###############################
	public Rectangle getBounds() {
		return bounds;
	}

	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
	}

	public Window getXWindow() {
		return xwindow;
	}

	public Window getXclient() {
		return xclient;
	}

	public void setXclient(Window xclient) {
		this.xclient = xclient;
	}
}
