package ch.admin.vbs.cube.core.vm;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ShellUtil;

public class VmAudioControl {
	private static final Logger LOG = LoggerFactory.getLogger(VmAudioControl.class);

	public enum Type {
		AUDIO("Sink Input", "sink-input"), MIC("Source Output", "source-output");
		public final String header;
		public final String cmd;

		Type(String header, String cmd) {
			this.header = header;
			this.cmd = cmd;
		}
	}

	public synchronized AudioEntry getAudio(String vmId, Type type) {
		return getVolumeEntry(vmId, type);
	}
	
	public synchronized void setMainVolume(int volPC) {
		ShellUtil pacmd = new ShellUtil();
		try {
			pacmd.run("pactl", "set-sink-volume", "0", volPC + "%");
		} catch (Exception e) {
			LOG.error("Failed to set volume", e);
		}
	}

	public synchronized void setVolume(String vmId, final Type type, int volPC) {
		final AudioEntry volEntry = getVolumeEntry(vmId, type);
		if (volEntry != null) {
			ShellUtil pacmd = new ShellUtil();
			try {
				pacmd.run("pactl", "set-" + type.cmd + "-volume", volEntry.getIndex() + "", volPC + "%");
			} catch (Exception e) {
				LOG.error("Failed to set volume", e);
			}
		}
	}

	public synchronized void setMuted(String vmId, final Type type, final boolean muted) {
		final AudioEntry ve = getVolumeEntry(vmId, type);
		if (ve != null) {
			ShellUtil pacmd = new ShellUtil();
			try {
				pacmd.run("pactl", "set-" + type.cmd + "-mute", ve.getIndex() + "", muted + "");
			} catch (Exception e) {
				LOG.error("Failed to set mute", e);
			}
		}
	}

	private AudioEntry getVolumeEntry(String vmId, Type type) {
		// find corresponding channels using 'pactl' command
		ShellUtil pacmd = new ShellUtil();
		try {
			pacmd.run("pactl", "list");
			StringBuffer sb = pacmd.getStandardOutput();
			StringReader r = new StringReader(sb.toString());
			BufferedReader rr = new BufferedReader(r);
			String line = null;
			Pattern titleRegex = Pattern.compile("^([\\w ]+) #(\\d+)$");
			// volume regex only parse first audio canal (it may be several of
			// them but with the same value)
			Pattern volumeRegex = Pattern.compile("^\\s+Volume:\\s+0:\\s*(\\d+)%.*$");
			Pattern muteRegex = Pattern.compile("^\\s+Mute:\\s+(\\w+)$");
			String currentHeader = null;
			String currentIndex = null;
			String currentVolume = null;
			String currentMute = null;
			boolean vmIdFound = false;
			boolean volumeFound = false;
			boolean muteFound = false;
			boolean headerFound = false;
			while ((line = rr.readLine()) != null) {
				Matcher titleMatch = titleRegex.matcher(line);
				// lookup for header (Sink, Source, Module, ...)
				if (titleMatch.matches()) {
					currentHeader = titleMatch.group(1);
					currentIndex = titleMatch.group(2);
					currentVolume = null;
					currentMute = null;
					vmIdFound = false;
					volumeFound = false;
					muteFound = false;
					headerFound = type.header.equals(currentHeader);
				}
				/**
				 * lookup for VM's ID in the paragraph (where it is exactly
				 * found may vary with VirtualBox version).
				 */
				if (headerFound && line.contains(vmId)) {
					vmIdFound = true;
				}
				// lookup for volume value
				if (headerFound) {
					Matcher volumeMatch = volumeRegex.matcher(line);
					if (volumeMatch.matches()) {
						volumeFound = true;
						currentVolume = volumeMatch.group(1);
					}
				}
				// lookup for mute value
				if (headerFound) {
					Matcher muteMatch = muteRegex.matcher(line);
					if (muteMatch.matches()) {
						muteFound = true;
						currentMute = muteMatch.group(1);
					}
				}
				//
				if (headerFound && vmIdFound && volumeFound && muteFound) {
					// create and return audio entry
					AudioEntry entry = new AudioEntry(Integer.parseInt(currentIndex), Integer.parseInt(currentVolume), "yes".equalsIgnoreCase(currentMute));
					return entry;
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to execute pactl or parse its output", e);
		}
		return null;
	}
}