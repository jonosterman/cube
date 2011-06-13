package ch.admin.vbs.cube.core;

import ch.admin.vbs.cube.common.UuidGenerator;

/**
 * Since cube UI is stateless, result of UI request (confirmation dialog, PIN
 * dialog, etc) are not returned in form of a method return value but via a
 * callback object.
 * 
 * This is required in order to be able to lock the screen (user removes its
 * token) at any time, even a confirmation dialog is currently displayed.
 */
public abstract class AbstractUICallback implements IUICallback {
	public final String id;

	public AbstractUICallback() {
		this.id = UuidGenerator.generate();
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return String.format("UICallback [%s]", id);
	}

}
