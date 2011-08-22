package ch.admin.vbs.cube.core.impl.scauthmodule;

import ch.admin.vbs.cube.core.AuthModuleEvent;
import ch.admin.vbs.cube.core.AuthModuleEvent.AuthEventType;
import ch.admin.vbs.cube.core.impl.scauthmodule.AbstractState.ScAuthStateTransition;

class StateWatchdog implements Runnable {
	/**
	 * 
	 */
	private final ScAuthModule scAuthModule;

	/**
	 * @param scAuthModule
	 */
	public StateWatchdog(ScAuthModule scAuthModule) {
		this.scAuthModule = scAuthModule;
	}

	@Override
	public void run() {
		while (this.scAuthModule.running) {
			// get a reference on active state
			AbstractState tstate = this.scAuthModule.activeState;
			// test if deadline is defined and expired
			if (tstate != null && tstate.deadline != 0 && tstate.deadline < System.currentTimeMillis()) {
				ScAuthModule.LOG.debug("Abort state [{}] due to timeout", tstate);
				tstate.deadline = 0; // reset deadline to
				if (tstate == this.scAuthModule.getStateInstance(WaitPasswordState.class)) {
					this.scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED_USERTIMEOUT, null, null, null));
				} else {
					this.scAuthModule.setAbortReason(new AuthModuleEvent(AuthEventType.FAILED_CARDTIMEOUT, null, null, null));
				}
				this.scAuthModule.enqueue(ScAuthStateTransition.ABORT_AUTH);
			}
			// log..
			if (tstate != null && tstate.deadline > 0) {
				ScAuthModule.LOG.debug("Monitor state [{}] : remaining {} ms", tstate, tstate.deadline - System.currentTimeMillis());
			}
			// sleep
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				ScAuthModule.LOG.error("", e);
			}
		}
	}
}