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

package ch.admin.vbs.cube.client.wm.xrandx.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcpiListener implements Runnable {
	private boolean running;
	private Process proc;
	private BufferedReader bufferedStdOutput;
	private static final Logger LOG = LoggerFactory.getLogger(AcpiListener.class);
	private ArrayList<IAcpiEventListener> listeners = new ArrayList<AcpiListener.IAcpiEventListener>(2);

	public enum AcpiEventType {
		EXTERN_DISPLAY_BUTTON, LID_EVENT
	}

	public void start() {
		new Thread(this, "ACPI Listen").start();
	}

	public void addListener(IAcpiEventListener l) {
		listeners.add(l);
	}

	public void removeListener(IAcpiEventListener l) {
		listeners.remove(l);
	}

	@Override
	public void run() {
		running = true;
		try {
			ProcessBuilder pb = new ProcessBuilder("acpi_listen");
			// start process
			proc = pb.start();
			// create BufferedReaders to read standard and error output
			bufferedStdOutput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			Pattern screenSwitchHotkeyPtrn = Pattern.compile("ibm/hotkey HKEY 00000080 00001007");
			Pattern lidOpenedPtrn = Pattern.compile("button/lid LID 00000080 .*");
			while (running) {
				String line = bufferedStdOutput.readLine();
				LOG.debug("Got ACPI command [{}]", line);
				if (line != null) {
					if (screenSwitchHotkeyPtrn.matcher(line).matches()) {
						fireEvent(new AcpiEvent(AcpiEventType.EXTERN_DISPLAY_BUTTON));
					} else if (lidOpenedPtrn.matcher(line).matches()) {
						fireEvent(new AcpiEvent(AcpiEventType.LID_EVENT));
					}
				}
			}
		} catch (Exception e) {
			LOG.error("ACPI Listener exit abnormaly", e);
		}
	}

	private void fireEvent(AcpiEvent acpiEvent) {
		// notify listeners
		for (IAcpiEventListener l : listeners) {
			l.acpi(acpiEvent);
		}
	}

	public class AcpiEvent {
		private final AcpiEventType type;

		public AcpiEvent(AcpiEventType type) {
			this.type = type;
		}

		public AcpiEventType getType() {
			return type;
		}
	}

	public interface IAcpiEventListener {
		void acpi(AcpiEvent e);
	}
}
