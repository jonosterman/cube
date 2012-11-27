/**
 * Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.admin.vbs.cube.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.vbs.cube.common.CubeException;
import ch.admin.vbs.cube.core.CubeClientCoreProperties;
import ch.admin.vbs.cube.core.ITokenDevice;
import ch.admin.vbs.cube.core.ITokenListener;
import ch.admin.vbs.cube.core.impl.TokenEvent.EventType;

public class ScTokenDevice implements ITokenDevice, Runnable {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ScTokenDevice.class);
	private static final String SC_SMARTCARDIO_LIBRARY_PROPERTY = "SCAdapter.smartcardioLibrary";
	private static final int RETRY_COUNT = 10;
	private String smartcardioLibraryPath;
	private CardTerminal terminal;
	private Thread thread;
	private boolean running;
	private ArrayList<ITokenListener> listeners = new ArrayList<ITokenListener>();
	private boolean tokenState = false;

	public ScTokenDevice() throws CubeException {
		// get library path from configuration file
		smartcardioLibraryPath = CubeClientCoreProperties.getProperty(SC_SMARTCARDIO_LIBRARY_PROPERTY);
		if (smartcardioLibraryPath == null) {
			throw new CubeException("Missing parameter [" + SC_SMARTCARDIO_LIBRARY_PROPERTY + "]");
		}
		// Initialize the smart-card terminal
		try {
			Properties p = System.getProperties();
			p.put("sun.security.smartcardio.library", smartcardioLibraryPath);
			// 2010.10.01/drf: just after booting, it seems that our reference
			// reader/pscd need some time to be up-and-running. Terminal
			// creation failed randomly along reboots. Therefore we introduce
			// this piece of code to retry to connect the terminal several times
			// before giving up.
			int retry = RETRY_COUNT;
			Exception savedEx = null;
			while (retry >= 0 && terminal == null) {
				try {
					TerminalFactory factory = TerminalFactory.getInstance("PC/SC", null);
					List<CardTerminal> terms = factory.terminals().list();
					LOG.debug("Found [{}] terminals", terms.size());
					if (terms.size() == 0) {
						// no smart card found. terminal variable will be 'null'
						LOG.error("No smart-card terminal found");
					} else if (terms.size() > 1) {
						terminal = terms.get(0);
						LOG.warn("Use first of [{}] terminals [{}]", terms.size(), terminal.getName());
					} else {
						terminal = terms.get(0);
						LOG.debug("Smart card terminal found [{}]", terminal.getName());
					}
				} catch (Exception e) {
					savedEx = e;
					if (--retry >= 0) {
						LOG.warn("Retry [{}] connecting terminal in 2 seconds.. [{}]", retry, e.getMessage());
						if (retry == 0) {
							LOG.debug("Detailed exception", e);
						}
						Thread.sleep(2000);
					}
				}
			}
			// if no terminal has been found after all these attempts due to an
			// exception, we fire it.
			if (terminal == null && savedEx != null) {
				throw savedEx;
			}
		} catch (Exception e) {
			throw new CubeException("Problem creating card terminal.", e);
		}
	}

	@Override
	public void run() {
		// initial state
		tokenState = false;
		// check if token is inserted or removed in a loop since we do not get
		// 'events' from hardware about it.
		while (running) {
			// get actual token state (present or not)
			boolean newState = false;
			try {
				newState = terminal.isCardPresent();
			} catch (CardException e) {
				LOG.error("Failed to get token state", e);
			}
			// check is state changed and notify listener accordingly
			if (newState != tokenState) {
				tokenState = newState;
				fireStateChanged(newState);
			}
			// wait until next check
			try {
				Thread.sleep(700);
			} catch (Exception e) {
				LOG.error("Epic fail", e);
			}
		}
		// nullify thread, so we could re-start this device
		thread = null;
	}

	private void fireStateChanged(boolean newState) {
		LOG.debug("Token state changed [{}]. Fire event.", newState);
		TokenEvent event = new TokenEvent(newState ? EventType.TOKEN_INSERTED : EventType.TOKEN_REMOVED);
		for (ITokenListener l : listeners) {
			l.notifyTokenEvent(event);
		}
	}


	public synchronized void start() {
		if (terminal == null) {
			LOG.error("No compatible smart-card terminal has been detected. Check your configuration.");
		} else if (thread == null) {
			running = true;
			thread = new Thread(this, "ScTokenDevice");
			thread.start();
		} else {
			LOG.error("ScTokenDevice is already started");
		}
	}
	
	public void setup() {}

	public synchronized void stop() {
		running = false;
	}

	@Override
	public void addListener(ITokenListener l) {
		listeners.add(l);
	}
	@Override
	public boolean isTokenReady() {
		return tokenState;
	}
}
