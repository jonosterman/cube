package ch.admin.cube.wsclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CubeWSClientConfig {
	private static final Logger LOG = LoggerFactory.getLogger(CubeWSClientConfig.class);
	private static CubeWSClientConfig cfg = new CubeWSClientConfig();
	private Properties props;

	private CubeWSClientConfig() {
		InputStream is = getClass().getResourceAsStream("/cubewsclient-config.xml");
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
