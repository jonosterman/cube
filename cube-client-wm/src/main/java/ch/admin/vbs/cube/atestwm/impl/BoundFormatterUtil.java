package ch.admin.vbs.cube.atestwm.impl;

import java.awt.Rectangle;

public class BoundFormatterUtil {
	public static final String format(Rectangle bnd) {
		return String.format("(%d:%d)(%dx%d)", bnd.x,bnd.y,bnd.width,bnd.height);
	}
}
