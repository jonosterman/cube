package ch.admin.vbs.cube.core.vm;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.shell.ShellUtil;

public class VmAudioControl {
	private static final Logger LOG = LoggerFactory.getLogger(VmAudioControl.class);
	private HashMap<String, AudioEntry> volumeEntries = new HashMap<String, AudioEntry>();

	public enum Type {
		AUDIO("Sink Input", "sink-input"), MIC("Source Output", "source-output");
		public final String header;
		public final String cmd;

		Type(String header, String cmd) {
			this.header = header;
			this.cmd = cmd;
		}
	}

	public synchronized AudioEntry getAudio(String key, Type type) {
		return getVolumeEntry(key, type);
	}

	public synchronized void setVolume(String key, final Type type, int vol) {
		final AudioEntry ve = getVolumeEntry(key, type);
		if (ve != null) {
			int v = (int) (65536d * (double) (vol / 100d));
			if (v > 65536) {
				vol = 100;
				v = 65536;
			}
			final String s = String.format("%05x", v);
			ShellUtil pacmd = new ShellUtil();
			try {
				pacmd.run("pacmd", "set-" + type.cmd + "-volume", ve.index + "", "0x" + s);
				ve.setVolume(vol);
			} catch (Exception e) {
			}
		}
	}

	public synchronized void setMuted(String key, final Type type, final boolean muted) {
		final AudioEntry ve = getVolumeEntry(key, type);
		if (ve != null) {
			ShellUtil pacmd = new ShellUtil();
			try {
				pacmd.run("pacmd", "set-" + type.cmd + "-mute", ve.index + "", muted + "");
				ve.setMuted(muted);
			} catch (Exception e) {
				LOG.error(e.toString());
			}
		}
	}

	private AudioEntry getVolumeEntry(String key, Type type) {
		if (volumeEntries.get(key + type) != null) {
			return volumeEntries.get(key + type);
		}
		int pid = getPID(key);
		ShellUtil pacmd = new ShellUtil();
		try {
			pacmd.run("pactl", "list");
		} catch (Exception e) {
			LOG.error(e.toString());
		}
		StringBuffer sb = pacmd.getStandardOutput();
		StringReader r = new StringReader(sb.toString());
		BufferedReader rr = new BufferedReader(r);
		try {
			String inp = null;
			int index = -1;
			int volume = -1;
			boolean muted = false;
			boolean proc = false;
			while ((inp = rr.readLine()) != null) {
				if (inp.contains(type.header)) {
					index = Integer.parseInt(inp.replace(type.header, "").replace("#", "").trim());
					proc = true;
					continue;
				} else if (!proc) {
					continue;
				}
				if (proc) {
					if (inp.contains("Volume:")) {
						inp = inp.replaceAll("Volume.*:", "");
						volume = Integer.parseInt(inp.replace("%", "").trim());
						continue;
					}
					if (inp.contains("Mute:")) {
						inp = inp.replace("Mute:", "");
						muted = !inp.trim().equals("no");
						continue;
					}
					if (inp.contains(pid + "")) {
						break;
					}
				}
			}
			if (index != -1) {
				volumeEntries.put(key + type, new AudioEntry(index, volume, muted));
				return volumeEntries.get(key + type);
			}
		} catch (Exception e) {
		}
		return null;
	}

	private int getPID(final String key) {
		ShellUtil pacmd = new ShellUtil();
		try {
			pacmd.run("pgrep", "-f", key);
		} catch (Exception e) {
			LOG.error(e.toString());
		}
		String outp = pacmd.getStandardOutput().toString();
		return Integer.parseInt(outp.trim());
	}
}