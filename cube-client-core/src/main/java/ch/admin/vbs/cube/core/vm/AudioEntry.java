package ch.admin.vbs.cube.core.vm;

/**
 * Holds information about a channel volume configuration. It is not
 * synchronized with the effective configuration in pulseaudio and should
 * therefore be re-fetched each time.
 */
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