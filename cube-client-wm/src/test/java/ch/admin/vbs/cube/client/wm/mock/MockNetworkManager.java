package ch.admin.vbs.cube.client.wm.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ch.admin.vbs.cube.common.shell.ShellUtil;
import ch.admin.vbs.cube.common.shell.ShellUtilException;
import ch.admin.vbs.cube.core.network.INetworkManager;

public class MockNetworkManager implements INetworkManager {
	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}
	
	@Override
	public List<String> getNetworkInterfaces() {
		ArrayList<String> list = new ArrayList<String>();
		ShellUtil su = new ShellUtil();
		try {
			su.run(null, ShellUtil.NO_TIMEOUT, "ifconfig");
			for(String line : su.getStandardOutput().toString().split("\n")) {
				if (line.startsWith("eth") | line.startsWith("wlan")) {
					list.add(line.split(" +",2)[0]);					
				}
			}
		} catch (ShellUtilException e) {
			e.printStackTrace();
		}		
		return list;
	}
	
	public static void main(String[] args) {
		new MockNetworkManager().getNetworkInterfaces();
	}

	@Override
	public NetworkConnectionState getState() {
		return NetworkConnectionState.CONNECTED_TO_CUBE;
	}

	@Override
	public void addListener(Listener l) {
	}

	@Override
	public void removeListener(Listener l) {
	}
}
