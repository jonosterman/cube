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

public class Session {
	private static final Logger LOG = LoggerFactory.getLogger(Session.class);
	private static TransferContainerFactory trfFactory;
	private Container trfContainer;
	private CubeKeyring keyring;

	public Session() {
	}

	/**
	 * First time user initialize the session (must be followed by a call to
	 * activate())
	 * 
	 * @param cFactory
	 */
	public void init(IIdentityToken id, IContainerFactory cFactory) {
		try {
			LOG.debug("Session init:");
			LOG.debug(" - (1/4) transfer factory");
			trfFactory = new TransferContainerFactory();		
			trfFactory.setContainerFactory(cFactory);
			LOG.debug(" - (2/4) transfer");
			trfContainer = trfFactory.initTransfer(id);
			LOG.debug(" - (3/4) keyring");
			keyring = new CubeKeyring(cFactory);
			keyring.open(id, trfContainer.getMountpoint());
			LOG.debug(" - (4/4) Session is ready");
		} catch (IOException e) {
			LOG.error("Failed to init session",e);
		} catch (ContainerException e) {
			LOG.error("Failed to init session",e);
		} catch (KeyringException e) {
			LOG.error("Failed to init session",e);
		}
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
}
