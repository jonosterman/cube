/**
 * Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
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
package ch.admin.vbs.cube.client.wm.demo.old;

import java.awt.Rectangle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.atestwm.impl.TabManager;

public class Meta {
	private static final Logger LOG = LoggerFactory.getLogger(Meta.class);

	public static void main(String[] args) throws Exception {
		// start Xephyr if not started
		// ! Xephyr MUST be started on DIAPLAY :9 before running this
		// Xephyr -ac -host-cursor -screen 640x480 -br -reset :9
		// ! this application must be started with env DISPLAY=:9
		// Simple Window Manager
		// ------------------- champ europ course traineau
		
		TabManager m = new TabManager();
		m.createPanel("test", new Rectangle(20,30,400,325));
		// -------------------
		System.out.println("done.");
	}
}
