package ch.admin.vbs.cube.client.wm.ui.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SpringLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.client.wm.utils.I18nBundleProvider;
import ch.admin.vbs.cube.client.wm.utils.IconManager;
import ch.admin.vbs.cube.core.crypt.CryptPasswordChanger;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.utils.SwingWorker;

public class CryptPasswordDialog extends CubeWizard {
	private static final Logger LOG = LoggerFactory.getLogger(PasswordDialog.class);
	private static final long serialVersionUID = 0L;
	private static final Dimension MINIMUM_DIALOG_SIZE = new Dimension(250, 250);	
	private ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
	// UI
	private JPasswordField oldPwdFld = new JPasswordField(20), newPwdFld = new JPasswordField(20), vfyPwdFld = new JPasswordField(20);
	private JPanel contentPnl;
	private JButton okButton;
	private JButton cancelButton;
	private List<JComponent> components = new ArrayList<JComponent>() {
		{
			add(new JLabel(resourceBundle.getString("passwordWizard.label.old")));
			add(oldPwdFld);
			add(new JLabel(resourceBundle.getString("passwordWizard.label.new")));
			add(newPwdFld);
			add(new JLabel(resourceBundle.getString("passwordWizard.label.verfiy")));
			add(vfyPwdFld);
		}
	};

	public CryptPasswordDialog(JFrame owner) {
		super(owner);
		setModal(false);
	}

	@Override
	protected JPanel createCenterContentPanel() {
		// create content elements
		contentPnl = new JPanel();
		JLabel iconLbl = new JLabel(IconManager.getInstance().getIcon("keys-icon48.png"));
		// layout
		SpringLayout layout = new SpringLayout();
		contentPnl.setLayout(layout);
		contentPnl.add(iconLbl);
		// constraints
		layout.putConstraint(SpringLayout.NORTH, iconLbl, 10, SpringLayout.NORTH, contentPnl);
		layout.putConstraint(SpringLayout.WEST, iconLbl, 20, SpringLayout.WEST, contentPnl);
		int yoffset = 0;
		for (JComponent c : components) {
			contentPnl.add(c);
			layout.putConstraint(SpringLayout.NORTH, c, yoffset, SpringLayout.NORTH, iconLbl);
			layout.putConstraint(SpringLayout.WEST, c, 20, SpringLayout.EAST, iconLbl);
			yoffset += 20;
		}
		setPreferredSize(MINIMUM_DIALOG_SIZE);
		return contentPnl;
	}

	private void notifyDialog(boolean success) {
		if (success) {
			this.dispose();
		} else {
			oldPwdFld.setEnabled(true);
			newPwdFld.setEnabled(true);
			vfyPwdFld.setEnabled(true);
			okButton.setEnabled(true);
			cancelButton.setEnabled(true);
			JOptionPane.showMessageDialog(this, "Cannot change password", "Password error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public ButtonPanel createButtonPanel() {
		okButton = addWizardAction(new AbstractAction(resourceBundle.getString("passwordWizard.buttoin.ok")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				oldPwdFld.setEnabled(false);
				newPwdFld.setEnabled(false);
				vfyPwdFld.setEnabled(false);
				okButton.setEnabled(false);
				cancelButton.setEnabled(false);
				if (!new String(newPwdFld.getPassword()).equals(new String(vfyPwdFld.getPassword()))) {
					notifyDialog(false);
				}
				SwingWorker<Boolean, Boolean> sw = new SwingWorker<Boolean, Boolean>() {
					@Override
					protected Boolean doInBackground() throws Exception {
						boolean ret = new CryptPasswordChanger().changePassword(new String(oldPwdFld.getPassword()), new String(vfyPwdFld.getPassword()));
						return ret;
					}

					@Override
					protected void done() {
						try {
							boolean ret = get();
							notifyDialog(ret);
						} catch (Exception e) {
							notifyDialog(false);
						}
					}
				};
				sw.execute();
			}
		}, true);
		cancelButton = addWizardAction(new AbstractAction(resourceBundle.getString("passwordWizard.button.cancel")) {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				notifyDialog(true);
			}
		});
		return super.createButtonPanel();
	}
}
