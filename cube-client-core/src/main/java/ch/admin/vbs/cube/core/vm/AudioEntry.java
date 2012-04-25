package ch.admin.vbs.cube.core.vm;

public class AudioEntry {
	private final int index;
	private final int volume;
	private final boolean muted;

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

	public boolean isMuted() {
		return muted;
	}
}