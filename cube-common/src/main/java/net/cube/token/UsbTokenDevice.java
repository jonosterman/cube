package net.cube.token;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsbTokenDevice implements ITokenDevice {
	private static final Logger LOG = LoggerFactory.getLogger(UsbTokenDevice.class);
	private HashMap<String, IIdentityToken> cache = new HashMap<String, IIdentityToken>();
	private Path dir;
	private ITokenPassphraseCallback pwdCallback;
	private ArrayList<ITokenDeviceListener> listeners = new ArrayList<ITokenDeviceListener>(1);

	public UsbTokenDevice() {
	}

	@Override
	public void addListener(ITokenDeviceListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(ITokenDeviceListener l) {
		listeners.remove(l);
	}

	private void fire(TokenDeviceEvent e) {
		for (ITokenDeviceListener l : listeners) {
			l.handle(e);
		}
	}

	public void setup(ITokenPassphraseCallback pwdCallback) {
		this.pwdCallback = pwdCallback;
	}

	public void start() {
		File base = new File("/media/" + System.getProperty("user.name"));
		dir = base.toPath();
		WatchService watcher;
		try {
			watcher = FileSystems.getDefault().newWatchService();
			dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
			UsbTokenDeviceWorker wk = new UsbTokenDeviceWorker(watcher);
			Thread t = new Thread(wk, "UsbTokenDeviceWorker");
			t.setDaemon(true);
			t.start();
			LOG.debug("USB key detector started..");
		} catch (IOException e) {
			LOG.error("Failed to register filesystem watcher for USB devices", e);
		}
	}

	private class UsbTokenDeviceWorker implements Runnable {
		private WatchService watcher;

		public UsbTokenDeviceWorker(WatchService watcher) {
			this.watcher = watcher;
		}

		@Override
		public void run() {
			while (true) {
				try {
					WatchKey w = watcher.take();
					for (WatchEvent<?> x : w.pollEvents()) {
						if (x.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
							tokenInserted((Path) x.context());
						} else if (x.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
							tokenRemoved((Path) x.context());
						}
					}
					w.reset();
				} catch (InterruptedException e) {
					LOG.error("Failed to proceed event", e);
				}
			}
		}

		private void tokenRemoved(Path context) {
			File usbDir = dir.resolve(context).toFile();
			IIdentityToken idt = cache.get(usbDir.getAbsolutePath());
			if (idt != null) {
				TokenDeviceEvent e = new TokenDeviceEvent(EventType.REMOVED, idt);
				fire(e);
			}
		}

		private void tokenInserted(Path context) {
			/**
			 * Look for keystore into new directory
			 */
			File usbDir = dir.resolve(context).toFile();
			/**
			 * Wait up to 5 seconds for USB device to be readable
			 */
			int x = 50;
			while (!usbDir.canExecute() && x-- > 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					LOG.error("Error", e);
				}
			}
			if (!usbDir.canExecute()) {
				/**
				 * Mounted directory (USB) should have been readable now. Return
				 * and print error.
				 */
				LOG.error("Failed to access directory [" + usbDir.getAbsolutePath() + "]");
				return;
			}
			try {
				for (File f : usbDir.listFiles()) {
					if (f.isFile() && f.getName().endsWith(".jks")) {
						// try to load JKS
						try {
							final char[] pwd = pwdCallback.getPassphrase();
							Builder builder = KeyStore.Builder.newInstance("JKS", null, f, new KeyStore.CallbackHandlerProtection(new PwdCallbackHandler(pwd)));
							KeyStore ks = builder.getKeyStore();
							IdentityToken idt = new IdentityToken(ks, builder, pwd);
							cache.put(usbDir.getAbsolutePath(), idt);
							TokenDeviceEvent e = new TokenDeviceEvent(EventType.INSERTED, idt);
							fire(e);
							// return: only load one JKS on this USB drive
							return;
						} catch (Exception e) {
							LOG.debug("Failed to load JKS [" + f.getAbsolutePath() + "]", e);
						}
					}
				}
			} catch (NullPointerException n) {
				LOG.error("Failed to load files from [" + usbDir.getAbsolutePath() + "] : java.lang.NullPointerException");
			}
		}
	}
}
