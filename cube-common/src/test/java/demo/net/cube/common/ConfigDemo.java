package demo.net.cube.common;

import net.cube.common.CubeConfig;

/**
 * Access sample configuration values from XML 
 */
public class ConfigDemo {
	public static void main(String[] args) throws Exception {
		String value = CubeConfig.getProperty("test");
		System.out.println("value : "+value);
	}
}
