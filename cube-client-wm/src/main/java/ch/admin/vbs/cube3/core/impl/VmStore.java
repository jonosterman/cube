package ch.admin.vbs.cube3.core.impl;

import java.util.ArrayList;

import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.network.INetManager;
import ch.admin.vbs.cube.core.network.INetManager.Listener;
import ch.admin.vbs.cube3.core.ILogin;
import ch.admin.vbs.cube3.core.IVmStore;
import ch.admin.vbs.cube3.core.IWSClientMgr;

public class VmStore implements IVmStore, Runnable, Listener {
	private ArrayList<VmStoreListener> listeners = new ArrayList<IVmStore.VmStoreListener>();
	private IContainerFactory cFactory;
	private IWSClientMgr ws;

	// ===============================================
	// IoC
	// ===============================================
	public void setup(IContainerFactory cFactory, IWSClientMgr ws, INetManager net) {
		this.cFactory = cFactory;
		this.ws = ws;
		net.addListener(this);
	}

	public void start() {
		Thread t = new Thread(this,"VmStore");
		t.setDaemon(true);
		t.start();
	}

	// ===============================================
	// Implements Runnable
	// ===============================================
	public void run() {
		// pool WebService for VM listing. refresh intern cache. notify changes to session manager
	}

	// ===============================================
	// Implements INetManage.Listener
	// ===============================================
	public void stateChanged(ch.admin.vbs.cube.core.network.INetManager.NetState old, ch.admin.vbs.cube.core.network.INetManager.NetState state) {
	};

	// ===============================================
	// Implements IVmStore
	// ===============================================
	@Override
	public ArrayList<Object> list() {
		return null;
	}

	@Override
	public void addVmStoreListener(VmStoreListener l) {
		listeners.add(l);
	}

	@Override
	public void removeVmStoreListener(VmStoreListener l) {
		listeners.add(l);
	}
}
