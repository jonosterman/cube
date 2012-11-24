package ch.admin.vbs.cube3.core.ui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ch.admin.vbs.cube.client.wm.utils.IconManager;

public class MessageFrame extends JFrame {
	public MessageFrame(String title) {
		setTitle(title);
		setUndecorated(true);
		JPanel panel = new JPanel(new BorderLayout());
		setContentPane(panel);
		panel.setBackground(Color.BLACK);
		JLabel l = new JLabel(IconManager.getInstance().getIcon("frame_bg.png"));
		panel.add(l, BorderLayout.CENTER);
		pack();
	}
}
