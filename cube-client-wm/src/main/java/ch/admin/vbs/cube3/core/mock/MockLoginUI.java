package ch.admin.vbs.cube3.core.mock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.impl.TokenEvent;
import ch.admin.vbs.cube3.core.ILoginUI;

public class MockLoginUI implements ILoginUI, ITokenListener {
	private static final Logger LOG = LoggerFactory.getLogger(MockLoginUI.class);
	private ArrayList<ILoginUIListener> listeners = new ArrayList<ILoginUI.ILoginUIListener>();
	private char[] passwd;
	private Executor exec = Executors.newCachedThreadPool();
	private Random rnd = new Random(System.currentTimeMillis());

	public MockLoginUI() {
		File file = new File(new File(System.getProperty("java.io.tmpdir")), "MockLoginUI_PIN.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			passwd = br.readLine().toCharArray();
		} catch (Exception e) {
			LOG.error("File ["+file.getAbsolutePath()+"] is not readable or does not contain a valid PIN. Exit.", e);
			System.exit(0);
		}
	}

	public void start() {
	}

	public void setup(ITokenDevice td) {
		td.addListener(this);
	}

	@Override
	public void addListener(ILoginUIListener l) {
		listeners.add(l);
	}

	@Override
	public void notifyTokenEvent(TokenEvent event) {
		switch (event.getType()) {
		case TOKEN_INSERTED:
			exec.execute(new Runnable() {
				@Override
				public void run() {
					int to = rnd.nextInt(10);
					LOG.debug("Provide dummy password in {} secs.",to);
					try {
						Thread.sleep(1000*to);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for (ILoginUIListener l : listeners) {
						l.setPassword(passwd);
					}
				}
			});
			break;
		default:
			break;
		}
	}

	@Override
	public void removeListener(ILoginUIListener l) {
		listeners.remove(l);
	}
}
