package ch.admin.vbs.cube.core;

public interface IUICallback {
	String getId();
	
	void process();
	
	void aborted();
}
