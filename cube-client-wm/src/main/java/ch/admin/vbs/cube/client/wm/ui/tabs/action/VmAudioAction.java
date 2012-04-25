package ch.admin.vbs.cube.client.wm.ui.tabs.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import ch.admin.vbs.cube.client.wm.client.VmHandle;
import ch.admin.vbs.cube.client.wm.ui.dialog.AudioDialog;
import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.core.vm.VmAudioControl;
import ch.admin.vbs.cube.core.vm.VmAudioControl.Type;

public class VmAudioAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private VmHandle vmHandle;
	private String vmName;

	public VmAudioAction(final VmHandle vmHandle, final String vmName) {
		super(I18nBundleProvider.getBundle().getString("vm.action.volume.text"));
		if (vmHandle == null) {
			throw new NullPointerException("vmHandle should not be null");
		}
		this.vmHandle = vmHandle;
		this.vmName = vmName;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final String key = vmHandle.getVmId();
				// final String key = "ubuntu11_10";
				final AudioDialog dial = new AudioDialog(null);
				final VmAudioControl vmc = new VmAudioControl();
				dial.initVolumeSlider("Vol " + vmName, vmc.getAudio(key, Type.AUDIO), new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						int vol = ((Integer) e.getSource());
						vmc.setVolume(key, Type.AUDIO, vol);
					}
				}, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						boolean b = ((Boolean) e.getSource());
						vmc.setMuted(key, Type.AUDIO, b);
					}
				});
				dial.initMicSlider("Mic " + vmName, vmc.getAudio(key, Type.MIC), new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						int vol = ((Integer) e.getSource());
						vmc.setVolume(key, Type.MIC, vol);
					}
				}, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						boolean b = ((Boolean) e.getSource());
						vmc.setMuted(key, Type.MIC, b);
					}
				});
				dial.displayWizard();
			}
		});
	}
}