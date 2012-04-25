package ch.admin.vbs.cube.client.wm.ui.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

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

import com.jidesoft.dialog.ButtonPanel;

public class AudioDialog extends CubeWizard {
	private static final long serialVersionUID = 1L;
	private static final Dimension MINIMUM_DIALOG_SIZE = new Dimension(400, 247);

	private enum Type {
		AUDIO(new JSlider(JSlider.HORIZONTAL, 0, 100, 50), new JCheckBox("muted"), new JLabel("Volume")), MIC(new JSlider(JSlider.HORIZONTAL, 0, 100, 50),
				new JCheckBox("muted"), new JLabel("Microphone"));
		public final JSlider slider;
		public final JCheckBox cbox;
		public final JLabel label;

		Type(JSlider slider, JCheckBox cbox, JLabel label) {
			this.slider = slider;
			this.cbox = cbox;
			this.label = label;
		}

		public void setMuted(boolean muted) {
			cbox.setSelected(muted);
			cbox.setEnabled(true);
			slider.setEnabled(!muted);
		}
	}

	public AudioDialog(JFrame owner) {
		super(owner);
	}

	public void initVolumeSlider(String name, AudioEntry ae, final ActionListener sliderListener, final ActionListener cboxListener) {
		Type.AUDIO.label.setText(name);
		initSliderInternal(Type.AUDIO, ae, sliderListener, cboxListener);
	}

	public void initMicSlider(String name, AudioEntry ae, final ActionListener sliderListener, final ActionListener cboxListener) {
		Type.MIC.label.setText(name);
		initSliderInternal(Type.MIC, ae, sliderListener, cboxListener);
	}

	private void initSliderInternal(final Type key, AudioEntry ae, final ActionListener sliderListener, final ActionListener cboxListener) {
		JSlider slider = key.slider;
		slider.setValue(ae.getVolume());
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int o = source.getValue();
					if (sliderListener != null)
						sliderListener.actionPerformed(new ActionEvent(new Integer(o), 1, ""));
				}
			}
		});
		// set initial checkbox & slider enable state
		key.setMuted(ae.isMuted());
		// add checkbox action listener
		key.cbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {				
				// update checkbox & slider enable state
				key.setMuted(key.cbox.isSelected());
				// notify listeners about checkbox state change
				if (cboxListener != null) {
					cboxListener.actionPerformed(new ActionEvent(new Boolean(key.cbox.isSelected()), 1, ""));
				}
			}
		});
	}

	public void initMicSlider(AudioEntry ae, String volName, final ActionListener sliderListener, final ActionListener cboxListener) {
		// TODO
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
		for (Type t : Type.values()) {
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
		addWizardAction(new AbstractAction("OK") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
			}
		}, true);
		return super.createButtonPanel();
	}
}