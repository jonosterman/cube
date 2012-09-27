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
package ch.admin.vbs.cube.core.vm;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ShellUtil;

/**
 * Use pulse audio commands to manage VM's audio volume (speakers and
 * microphone).
 */
public class VmAudioControl {
	private static final Logger LOG = LoggerFactory.getLogger(VmAudioControl.class);

	/**
	 * sink (speaker) and source (microphone)
	 */
	public enum Type {
		AUDIO("Sink Input", "sink-input"), MIC("Source Output", "source-output");
		public final String header;
		public final String cmd;

		Type(String header, String cmd) {
			this.header = header;
			this.cmd = cmd;
		}
	}

	/**
	 * @return the audio entry for a VM of the given type
	 */
	public synchronized AudioEntry getAudio(String vmId, Type type) {
		return getVolumeEntry(vmId, type);
	}

	/** Set main output volume (typically at startup) */
	public synchronized void setMainVolume(int volPC) {
		ShellUtil pacmd = new ShellUtil();
		try {
			pacmd.run("pactl", "set-sink-volume", "0", volPC + "%");
		} catch (Exception e) {
			LOG.error("Failed to set volume", e);
		}
	}

	/**
	 * Set a VM volume level for the given sink/source. Volume is specified in %
	 */
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

	/** Mute the given sink/source */
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
			int pid = getPID(vmId);
			pacmd.run("pactl", "list");
			// pacmd.run("ssh", "cube001_home.cube", "su cube -c 'pactl list'");
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
			String currentVolume = "100";
			String currentMute = "no";
			boolean vmIdFound = false;
			boolean headerFound = false;
			while ((line = rr.readLine()) != null) {
				Matcher headerMatch = titleRegex.matcher(line);
				// lookup for header (Sink, Source, Module, ...)
				if (headerMatch.matches()) {
					currentHeader = headerMatch.group(1);
					currentIndex = headerMatch.group(2);
					vmIdFound = false;
					headerFound = type.header.equals(currentHeader);
					currentVolume = "100"; // default since not always in 'pactl
											// list'
					currentMute = "no"; // default since not always in 'pactl
										// list'
				}
				/**
				 * lookup for VM's ID in the paragraph (where it is exactly
				 * found may vary with VirtualBox version).
				 */
				if (headerFound && line.contains("application.process.id = \"" + pid + "\"")) {
					vmIdFound = true;
				}
				// lookup for volume value
				if (headerFound) {
					Matcher volumeMatch = volumeRegex.matcher(line);
					if (volumeMatch.matches()) {
						currentVolume = volumeMatch.group(1);
					}
				}
				// lookup for mute value
				if (headerFound) {
					Matcher muteMatch = muteRegex.matcher(line);
					if (muteMatch.matches()) {
						currentMute = muteMatch.group(1);
					}
				}
				//
				if (headerFound && vmIdFound) {
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

	/**
	 * we need to find the VMs' PID in order to search the VM's channels in
	 * pactl's output. We tried to use 'application.name' but it is truncated
	 * and does not contains the whole VM's ID.
	 */
	private int getPID(final String key) {
		ShellUtil pacmd = new ShellUtil();
		try {
			// pacmd.run("ssh", "cube001_home.cube", "pgrep -f "+key);
			pacmd.run("pgrep", "-f", key);
		} catch (Exception e) {
			LOG.error(e.toString(), e);
		}
		String outp = pacmd.getStandardOutput().toString().trim();
		return outp.length() == 0 ? -1 : Integer.parseInt(outp);
	}
}