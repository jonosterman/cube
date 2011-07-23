/**
 * Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.admin.vbs.cube.client.wm.ui.wm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ch.admin.vbs.cube.client.wm.utils.IconManager;

public class BackgroundFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	public static final String TITLE_PREFIX = "Cube.BackgroundFrame#";

	public BackgroundFrame(String id, Rectangle bounds) {
		// title is important since in WindowsManager we search window by name
		super(TITLE_PREFIX + id);
		setUndecorated(true);
		JPanel panel = new JPanel(new BorderLayout());
		setContentPane(panel);
		getContentPane().setBackground(Color.BLACK);
		JLabel l = new JLabel(IconManager.getInstance().getIcon("frame_bg.png"));
		getContentPane().add(l, BorderLayout.CENTER);
		setPreferredSize(new Dimension(bounds.width, bounds.height));
		setLocation(bounds.x, bounds.y);
		pack();
	}
}
