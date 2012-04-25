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
				final String vmId = vmHandle.getVmId();
				final VmAudioControl vmc = new VmAudioControl();
				final AudioDialog dial = new AudioDialog(null, vmId, vmc);
				dial.displayWizard();
			}
		});
	}
}