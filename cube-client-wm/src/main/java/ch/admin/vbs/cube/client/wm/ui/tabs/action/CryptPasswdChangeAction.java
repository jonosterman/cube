package ch.admin.vbs.cube.client.wm.ui.tabs.action;

import java.awt.event.ActionEvent;

import ch.admin.vbs.cube.client.wm.client.IUserInterface;

public class CryptPasswdChangeAction extends CubeAbstractAction {
	private static final long serialVersionUID = 1L;
	private final IUserInterface userui;

	public CryptPasswdChangeAction(IUserInterface userui) {
		super("Change Encrypt Passwd", null);
		this.userui = userui;
	}

	@Override
	public void exec(ActionEvent actionEvent) {
		userui.showDiskPasswordChangeDialog();
	}

}
