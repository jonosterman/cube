
package ch.admin.vbs.cube.client.wm.mock;

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
		EXTERN_DISPLAY_BUTTON
	}

	public void start() {
		new Thread(this, "ACPI Listen");
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
			Pattern lidOpenedPtrn = Pattern.compile("mojojojo");
			while (running) {
				String line = bufferedStdOutput.readLine();
				if (lidOpenedPtrn.matcher(line).matches()) {
					fireEvent(new AcpiEvent(AcpiEventType.EXTERN_DISPLAY_BUTTON));
				}
			}
		} catch (Exception e) {
			LOG.error("ACPI Listener exit abnormaly");
		}
	}

	private void fireEvent(AcpiEvent acpiEvent) {
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
