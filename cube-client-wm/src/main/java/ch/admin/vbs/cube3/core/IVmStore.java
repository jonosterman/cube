package ch.admin.vbs.cube3.core;

import java.util.ArrayList;

public interface IVmStore {
	public static enum VmStoreEvent { LIST_CHANGED }
	ArrayList<Object> list();

	public void addVmStoreListener(VmStoreListener l);
	public void removeVmStoreListener(VmStoreListener l);
	
	public interface VmStoreListener {
		void process(VmStoreEvent l);
	}
}
