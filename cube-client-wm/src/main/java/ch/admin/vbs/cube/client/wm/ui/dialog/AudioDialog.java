package ch.admin.vbs.cube.client.wm.ui.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.core.vm.AudioEntry;
import ch.admin.vbs.cube.core.vm.VmAudioControl;
import ch.admin.vbs.cube.core.vm.VmAudioControl.Type;

import com.jidesoft.dialog.ButtonPanel;

public class AudioDialog extends CubeWizard {
	private static final long serialVersionUID = 1L;
	private static final Dimension MINIMUM_DIALOG_SIZE = new Dimension(400, 247);
	private final String vmId;
	private final VmAudioControl vmc;
	private final String vmName;

	private enum TypeUI {
		AUDIO, MIC;
		public JSlider slider;
		public JCheckBox cbox;
		public JLabel label;

		public void setMuted(boolean muted) {
			cbox.setSelected(muted);
			cbox.setEnabled(true);
			slider.setEnabled(!muted);
		}
	}

	public AudioDialog(JFrame owner, String vmId, VmAudioControl vmc, String vmName) {
		super(owner);
		this.vmId = vmId;
		this.vmc = vmc;
		this.vmName = vmName;
		// set initial sliders and checkbox values
		initSlider(TypeUI.AUDIO, vmc.getAudio(vmId, Type.AUDIO), Type.AUDIO);
		initSlider(TypeUI.MIC, vmc.getAudio(vmId, Type.MIC), Type.MIC);
	}

	private void initSlider(final TypeUI uiType, AudioEntry entry, final Type type) {
		uiType.slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
		uiType.cbox = new JCheckBox("muted");
		uiType.label = new JLabel("Volume for VM ["+vmName+"]");
		if (entry == null) {
			uiType.slider.setEnabled(false);
			uiType.cbox.setEnabled(false);
			return;
		}
		//
		JSlider slider = uiType.slider;
		slider.setValue(entry.getVolume());
		// add change listener
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int o = source.getValue();
					vmc.setVolume(vmId, type, new Integer(o));
				}
			}
		});
		// set initial checkbox & slider enable state
		uiType.setMuted(entry.isMuted());
		// add checkbox action listener
		uiType.cbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				boolean muted = uiType.cbox.isSelected();
				// update checkbox & slider enable state
				uiType.setMuted(muted);
				// set muted
				vmc.setMuted(vmId, type, muted);
			}
		});
	}

	@Override
	protected JPanel createCenterContentPanel() {
		ImageIcon icon = null;
		JPanel contentPnl = new JPanel();
		// layout
		SpringLayout layout = new SpringLayout();
		contentPnl.setLayout(layout);
		JLabel iconLb = new JLabel(icon);
		contentPnl.add(iconLb);
		layout.putConstraint(SpringLayout.NORTH, iconLb, 40, SpringLayout.NORTH, contentPnl);
		layout.putConstraint(SpringLayout.WEST, iconLb, 20, SpringLayout.WEST, contentPnl);
		int yoffset = 0;
		for (TypeUI t : TypeUI.values()) {
			JLabel label = t.label;
			JSlider slider = t.slider;
			JCheckBox cbox = t.cbox;
			contentPnl.add(label);
			contentPnl.add(slider);
			contentPnl.add(cbox);
			layout.putConstraint(SpringLayout.NORTH, label, 20, SpringLayout.NORTH, slider);
			layout.putConstraint(SpringLayout.WEST, label, 20, SpringLayout.WEST, slider);
			layout.putConstraint(SpringLayout.NORTH, cbox, -5, SpringLayout.NORTH, slider);
			layout.putConstraint(SpringLayout.WEST, cbox, 200, SpringLayout.WEST, slider);
			layout.putConstraint(SpringLayout.NORTH, slider, 0 + yoffset, SpringLayout.NORTH, iconLb);
			layout.putConstraint(SpringLayout.WEST, slider, 20, SpringLayout.EAST, iconLb);
			setMinimumSize(MINIMUM_DIALOG_SIZE);
			yoffset += 50;
		}
		return contentPnl;
	}

	@Override
	public ButtonPanel createButtonPanel() {
		addWizardAction(new AbstractAction(I18nBundleProvider.getBundle().getString("audiodialog.button.close")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
				dispose();
			}
		}, true);
		return super.createButtonPanel();
	}
}