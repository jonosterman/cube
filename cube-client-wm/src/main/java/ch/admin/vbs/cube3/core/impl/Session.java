package ch.admin.vbs.cube3.core.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.common.container.ContainerException;
import ch.admin.vbs.cube.common.container.IContainerFactory;
import ch.admin.vbs.cube.common.keyring.IIdentityToken;
import ch.admin.vbs.cube.common.keyring.impl.CubeKeyring;
import ch.admin.vbs.cube.common.keyring.impl.KeyringException;
import ch.admin.vbs.cube.core.impl.TransferContainerFactory;
import ch.admin.vbs.cube3.core.ISession;

public class Session implements ISession {
	private static final Logger LOG = LoggerFactory.getLogger(Session.class);
	private static TransferContainerFactory trfFactory;
	private Container trfContainer;
	private CubeKeyring keyring;
	private boolean initialized;
	private IIdentityToken id;
	private IContainerFactory cFactory;

	public Session() {
	}

	/**
	 * First time user initialize the session (must be followed by a call to
	 * activate()). Since this may take a while, we run it in a thread and set
	 * 'initialized' flag when done.
	 * 
	 * @param cFactory
	 */
	public void init(IIdentityToken id, IContainerFactory cFactory) {
		this.id = id;
		this.cFactory = cFactory;
		initialized = false;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					LOG.debug("Session init:");
					LOG.debug(" - (1/4) transfer factory");
					trfFactory = new TransferContainerFactory();
					trfFactory.setContainerFactory(Session.this.cFactory);
					LOG.debug(" - (2/4) transfer");
					trfContainer = trfFactory.initTransfer(Session.this.id);
					LOG.debug(" - (3/4) keyring");
					keyring = new CubeKeyring(Session.this.cFactory);
					keyring.open(Session.this.id, trfContainer.getMountpoint());
					LOG.debug(" - (4/4) Session is ready");
					initialized = true;
				} catch (IOException e) {
					LOG.error("Failed to init session", e);
				} catch (ContainerException e) {
					LOG.error("Failed to init session", e);
				} catch (KeyringException e) {
					LOG.error("Failed to init session", e);
				}
			}
		}, "Session init thread");
		t.start();
	}

	/**
	 * User unlocked the session with its token.
	 */
	public void activate() {
		LOG.debug("Session activated");
	}

	/**
	 * User removed its token
	 */
	public void lock() {
		LOG.debug("Session locked");
	}

	/**
	 * When user want requested to logout or the system is going to shutdown
	 */
	public void stop() {
		LOG.debug("Session stopped");
	}

	// ===============================================
	// Implements ISession
	// ===============================================
	@Override
	public void addListener(ISessionChangeListener l) {
	}

	@Override
	public void removeListener(ISessionChangeListener l) {
	}
}
