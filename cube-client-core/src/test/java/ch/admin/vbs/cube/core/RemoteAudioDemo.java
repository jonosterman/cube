package ch.admin.vbs.cube.core;

import ch.admin.vbs.cube.core.vm.AudioEntry;
import ch.admin.vbs.cube.core.vm.VmAudioControl;
import ch.admin.vbs.cube.core.vm.VmAudioControl.Type;

public class RemoteAudioDemo {
	public static void main(String[] args) throws Exception {
		new RemoteAudioDemo().startAndConnectVboxsrv();
	}

	public void startAndConnectVboxsrv() throws Exception {
		AudioEntry a1 = new VmAudioControl().getAudio("58ebf946-155d-cfd6-8600-3451c924", Type.AUDIO);
		System.out.println(a1);
		AudioEntry a2 = new VmAudioControl().getAudio("58ebf946-155d-cfd6-8600-3451c924", Type.MIC);
		System.out.println(a2);
		}
}