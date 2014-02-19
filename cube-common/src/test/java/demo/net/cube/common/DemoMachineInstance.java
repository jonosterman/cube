package demo.net.cube.common;

import net.cube.common.CubeConfig;
import net.cube.common.MachineUuid;

public class DemoMachineInstance {
	public static void main(String[] args) {
		System.out.println("cube dir : " + CubeConfig.getBaseDir().getAbsolutePath());
		//
		System.out.println("machine uuid : "+MachineUuid.getMachineUuid());
	}
}
