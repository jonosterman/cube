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

package ch.admin.vbs.cube.client.wm.client;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JFrame;

import ch.admin.vbs.cube.client.wm.ui.tabs.NavigationBar;
import ch.admin.vbs.cube.client.wm.ui.tabs.NavigationFrame;
import ch.admin.vbs.cube.client.wm.ui.wm.BackgroundFrame;

public class DemoCompleteClientTwoScreensNewLogin extends AbstractCubeDemoNewLogin {
	
	@Override
	public void initScreens() {
		JFrame display0 = new JFrame("cube frame");
		display0.setPreferredSize(new Dimension(800, 650));
		display0.setUndecorated(true);
		display0.pack();
		JFrame display1 = new JFrame("cube frame");
		display1.setPreferredSize(new Dimension(900, 700));
		display1.setUndecorated(true);
		display1.pack();
		// setup displays: get single screen or the nvidia twin view
		// screens, other things will not work
		int monitorCount = 2;
		cubeFrames = new JFrame[monitorCount];
		navBars = new NavigationBar[monitorCount];
		navBarFrames = new NavigationFrame[monitorCount];
		for (int i = 0; i < monitorCount; ++i) {
			// monitor dimension
			Rectangle monitorDim = null;
			if (i==0) {
				monitorDim = new Rectangle(50,50,display0.getWidth(),display0.getHeight());
			} else {
				monitorDim = new Rectangle(1000,150,display1.getWidth(),display1.getHeight());				
			}
			// create Cube Frames (each frame cover a monitor)
			cubeFrames[i] = new BackgroundFrame(i, monitorDim);
			// create navigation bar
			navBars[i] = new NavigationBar(i, monitorCount, cubeFrames[i]);
			navBarFrames[i] = navBars[i].getNavBar();
		}
	}
	
	public static void main(String[] args) throws Exception {
		DemoCompleteClientTwoScreensNewLogin d = new DemoCompleteClientTwoScreensNewLogin();
		d.startDemo();
	}

}
