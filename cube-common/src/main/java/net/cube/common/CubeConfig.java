package net.cube.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CubeConfig {
	private static final Logger LOG = LoggerFactory.getLogger(CubeConfig.class);
	private static CubeConfig cfg = new CubeConfig();
	private Properties props;

	private CubeConfig() {
		InputStream is = getClass().getResourceAsStream("/cube-config.xml");
		props = new Properties();
		try {
			props.loadFromXML(is);
			is.close();
		} catch (IOException e) {
			LOG.error("Failed to load configuration file", e);
		}
	}

	public static File getBaseDir() {
		String base = cfg.props.getProperty("base.dir");
		if (base == null) {
			return new File(new File(System.getProperty("user.home")), ".cube");
		} else {
			return new File(base);
		}
	}

	public static String getProperty(String key) {
		return cfg.props.getProperty(key);
	}

	public static int getPropertyAsInt(String key, int defValue) {
		return Integer.parseInt(cfg.props.getProperty(key, defValue + ""));
	}
}
