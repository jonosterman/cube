package ch.admin.vbs.cube.client.wm.xrandx;

import java.util.List;

public class XRScreen {
	private final String id;
	private final String state;
	private final int posy;
	private final int posx;
	private final XRResolution selectedResolution;
	private final String selectedFrequency;
	private final List<XRResolution> resolutions;

	@Override
	public String toString() {
		return String.format("[%s] %s , %dx%d", id, state, selectedResolution == null ? -1 : selectedResolution.width, selectedResolution == null ? -1
				: selectedResolution.height);
	}

	public XRScreen(String id, String state, int posx, int posy, List<XRResolution> resolutions, XRResolution selectedResolution, String selectedFrequency) {
		this.id = id;
		this.state = state;
		this.posx = posx;
		this.posy = posy;
		this.resolutions = resolutions;
		this.selectedResolution = selectedResolution;
		this.selectedFrequency = selectedFrequency;
	}

	public static class XRResolution {
		private final int width;
		private final List<String> freqs;
		private final int height;

		public XRResolution(int width, int height, List<String> freqs) {
			this.width = width;
			this.freqs = freqs;
			this.height = height;
		}
	}

	public String getId() {
		return id;
	}

	public boolean isConnected() {
		return "connected".equals(state);
	}
	public int getPosy() {
		return posy;
	}

	public int getPosx() {
		return posx;
	}

	public int getCurrentWidth() {
		if (selectedResolution == null) {
			return 0;
		} else {
			return selectedResolution.width;
		}
	}

	public int getCurrentHeight() {
		if (selectedResolution == null) {
			return 0;
		} else {
			return selectedResolution.height;
		}
	}

	public XRResolution getSelectedResolution() {
		return selectedResolution;
	}

	public String getSelectedFrequency() {
		return selectedFrequency;
	}

	public String getState() {
		return state;
	}

	public List<XRResolution> getResolutions() {
		return resolutions;
	}
}
