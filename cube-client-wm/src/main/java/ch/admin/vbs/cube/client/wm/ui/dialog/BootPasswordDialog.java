
package ch.admin.vbs.cube.client.wm.ui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
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

public class BootPasswordDialog extends CubeWizard {
	private static final Logger LOG = LoggerFactory.getLogger(PasswordDialog.class);
	private static final long serialVersionUID = 0L;
	private static final Dimension MINIMUM_DIALOG_SIZE = new Dimension(250, 300);
	private ResourceBundle resourceBundle = I18nBundleProvider.getBundle();
	// UI
	private JPasswordField oldPwdFld;
	private JPasswordField newPwdFld;
	private JPasswordField vfyPwdFld;
	private JPanel contentPnl;
	private JButton okButton;
	private JButton cancelButton;
	private boolean isNewPwdValid;
	private JLabel messageLbl;
	private ArrayList<BootPasswordListener> listeners = new ArrayList<BootPasswordDialog.BootPasswordListener>(2);
	private ExecutorService execs = Executors.newCachedThreadPool();
	
	public BootPasswordDialog(JFrame owner) {
		super(owner);
		setModal(false);
	}

	@Override
	protected JPanel createCenterContentPanel() {
		// create content elements
		contentPnl = new JPanel();
		JLabel iconLbl = new JLabel(IconManager.getInstance().getIcon("keys-icon48.png"));
		JLabel oldLbl = new JLabel(resourceBundle.getString("passwordWizard.label.old"));
		JLabel newLbl = new JLabel(resourceBundle.getString("passwordWizard.label.new"));
		JLabel vfyLbl = new JLabel(resourceBundle.getString("passwordWizard.label.verify"));
		messageLbl = new JLabel("");
		messageLbl.setForeground(Color.RED);
		oldPwdFld = new JPasswordField(20);
		newPwdFld = new JPasswordField(20);
		vfyPwdFld = new JPasswordField(20);
		// layout
		SpringLayout layout = new SpringLayout();
		contentPnl.setLayout(layout);
		contentPnl.add(iconLbl);
		contentPnl.add(oldLbl);
		contentPnl.add(oldPwdFld);
		contentPnl.add(newLbl);
		contentPnl.add(newPwdFld);
		contentPnl.add(vfyLbl);
		contentPnl.add(vfyPwdFld);
		contentPnl.add(messageLbl);
		// constraints
		layout.putConstraint(SpringLayout.NORTH, iconLbl, 40, SpringLayout.NORTH, contentPnl);
		layout.putConstraint(SpringLayout.WEST, iconLbl, 20, SpringLayout.WEST, contentPnl);
		//
		layout.putConstraint(SpringLayout.WEST, messageLbl, 20, SpringLayout.EAST, iconLbl);
		layout.putConstraint(SpringLayout.NORTH, messageLbl, -30, SpringLayout.NORTH, iconLbl);
		layout.putConstraint(SpringLayout.NORTH, oldLbl, 0, SpringLayout.NORTH, iconLbl);
		layout.putConstraint(SpringLayout.WEST, oldLbl, 20, SpringLayout.EAST, iconLbl);
		layout.putConstraint(SpringLayout.NORTH, oldPwdFld, 20, SpringLayout.NORTH, iconLbl);
		layout.putConstraint(SpringLayout.WEST, oldPwdFld, 20, SpringLayout.EAST, iconLbl);
		layout.putConstraint(SpringLayout.NORTH, newLbl, 50, SpringLayout.NORTH, iconLbl);
		layout.putConstraint(SpringLayout.WEST, newLbl, 20, SpringLayout.EAST, iconLbl);
		layout.putConstraint(SpringLayout.NORTH, newPwdFld, 70, SpringLayout.NORTH, iconLbl);
		layout.putConstraint(SpringLayout.WEST, newPwdFld, 20, SpringLayout.EAST, iconLbl);
		layout.putConstraint(SpringLayout.NORTH, vfyLbl, 100, SpringLayout.NORTH, iconLbl);
		layout.putConstraint(SpringLayout.WEST, vfyLbl, 20, SpringLayout.EAST, iconLbl);
		layout.putConstraint(SpringLayout.NORTH, vfyPwdFld, 120, SpringLayout.NORTH, iconLbl);
		layout.putConstraint(SpringLayout.WEST, vfyPwdFld, 20, SpringLayout.EAST, iconLbl);
		setPreferredSize(MINIMUM_DIALOG_SIZE);
		//
		newPwdFld.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				updateOkButton();
			}
		});
		vfyPwdFld.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				updateOkButton();
			}
		});
		return contentPnl;
	}

	protected void updateOkButton() {
		String a = new String(newPwdFld.getPassword());
		String b = new String(vfyPwdFld.getPassword());
		boolean pwdShort = a.length() <= 8;
		// 
		boolean badChars = !a.matches(CryptPasswordChanger.US_KBD_ALLOWED_REGEX);
		if (pwdShort) {
			messageLbl.setText(resourceBundle.getString("passwordWizard.warn.short"));
			LOG.debug("Password is too short");
		} else if (badChars) {
			messageLbl.setText(resourceBundle.getString("passwordWizard.warn.badchars"));
			LOG.debug("Bad char(s) in submitted password");
		} else if (!a.equals(b)) {
			messageLbl.setText("");
			LOG.debug("good password");
		}
		isNewPwdValid = !pwdShort && !badChars;
		okButton.setEnabled(isNewPwdValid && a.equals(b));
	}

	private void notifyDialog(boolean success) {
		if (success) {
			this.dispose();
			notifyListenersAndClose();
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
		okButton = addWizardAction(new AbstractAction(resourceBundle.getString("passwordWizard.button.ok")) {
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
		okButton.setEnabled(false);
		cancelButton = addWizardAction(new AbstractAction(resourceBundle.getString("passwordWizard.button.cancel")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				notifyDialog(true);
			}
		});
		return super.createButtonPanel();
	}
	
	private void notifyListenersAndClose() {
		execs.execute(new Runnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				List<BootPasswordListener> cloned;
				synchronized (listeners) {
					cloned = (List<BootPasswordListener>) listeners.clone();
				}
				for (BootPasswordListener listener : cloned) {
						listener.closed();
				}
			}
		});
	}
	
	public void addPasswordDialogListener(BootPasswordListener listener) {
		synchronized (listener) {
			listeners .add(listener);
		}
	}

	public void removePasswordDialogListener(BootPasswordListener listener) {
		synchronized (listener) {
			listeners.remove(listener);
		}
	}
	
	public interface BootPasswordListener {
		void closed();
	}

	public static void main(String[] args) {
		BootPasswordDialog x = new BootPasswordDialog(null);
		x.displayWizard();
	}
}
