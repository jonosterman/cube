package ch.admin.vbs.cube.core.vm;

public class AudioEntry {
	int index;
	int volume;
	boolean muted;

	public AudioEntry(int index, int volume, boolean muted) {
		this.index = index;
		this.volume = volume;
		this.muted = muted;
	}

	public int getIndex() {
		return index;
	}

	public int getVolume() {
		return volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean mute) {
		this.muted = mute;
	}
}