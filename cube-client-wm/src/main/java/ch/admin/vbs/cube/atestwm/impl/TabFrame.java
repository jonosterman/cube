package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Rectangle;

import javax.swing.JFrame;

public class TabFrame extends JFrame {
	private Rectangle bounds = new Rectangle(0, 0, 10, 10);

	public TabFrame(String title) {
		super(title);
	}

	public Rectangle getBoundsAbsolute() {
		return bounds;
	}

	public void setBoundsAbsolute(Rectangle bounds) {
		this.bounds = bounds;
	}
}
