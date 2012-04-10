package ch.admin.vbs.cube.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;

import ch.admin.vbs.cube.common.CubeClassification;
import ch.admin.vbs.cube.common.container.Container;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.list.VmDescriptor;
import ch.admin.vbs.cube.core.vm.list.VmDescriptor.LocalConfig;
import ch.admin.vbs.cube.core.vm.list.VmDescriptor.RemoteConfig;
import ch.admin.vbs.cube.core.vm.vbox.VBoxConfig;
import ch.admin.vbs.cube.core.vm.vbox.VBoxConfig.VBoxOption;

public class MockContainerUtil {

	public Vm createTestVm(String name) throws Exception {
		VmDescriptor desc = new VmDescriptor();
		Vm vm = new Vm(desc);
		LocalConfig l = desc.getLocalCfg();
		RemoteConfig r = desc.getRemoteCfg();
		l.setRuntimeContainerUid("test-rt-"+name);
		l.setVmContainerUid("test-vm-"+name);
		l.setProperty("test-vm", "true");
		r.setClassification(CubeClassification.UNCLASSIFIED);
		r.setCfgVersion("0.1");
		r.setDescription("test VM '"+name+"'");
		r.setDomain("test");
		r.setId("test-id-"+name);
		r.setName(name);
		r.setType("test-type");
		// Initialize a dummy directory structure to hold all VM's files
		File tmp = new File(System.getProperty("java.io.tmpdir"));
		File vmBase = new File(tmp,"test-"+name);
		vm.setExportFolder(new File(vmBase,"export"));
		vm.setImportFolder(new File(vmBase,"import"));
		Container rtContainer = new Container();
		rtContainer.setContainerFile(new File(vmBase,"rtcontainer.data"));
		rtContainer.setMountpoint(new File(vmBase,"rtcontainer_mount"));
		vm.setRuntimeContainer(rtContainer);
		Container vmContainer = new Container();
		vmContainer.setContainerFile(new File(vmBase,"vmcontainer.data"));
		vmContainer.setMountpoint(new File(vmBase,"vmcontainer_mount"));
		vm.setVmContainer(vmContainer);
		// Create necessary directories on the disk
		rtContainer.getMountpoint().mkdirs();
		vmContainer.getMountpoint().mkdirs();
		vm.getImportFolder().mkdirs();
		vm.getExportFolder().mkdirs();
		// props
		VBoxConfig vc = new VBoxConfig(vmContainer, rtContainer);
		vc.setOption(VBoxOption.Nic1, "disabled");
		vc.setOption(VBoxOption.Nic2, "disabled");
		vc.setOption(VBoxOption.Nic3, "disabled");
		vc.setOption(VBoxOption.Nic4, "disabled");
		vc.setOption(VBoxOption.IoApic, "true");
		vc.setOption(VBoxOption.Disk1File, "disk1");
		vc.setOption(VBoxOption.OsType, "Ubuntu");
		vc.setOption(VBoxOption.Acpi, "true");		
		vc.setOption(VBoxOption.BaseMemory, "400");
		vc.setOption(VBoxOption.VideoMemory, "128");
		vc.setOption(VBoxOption.Audio, "on");
		vc.setOption(VBoxOption.Pae, "true");
		vc.save();
		// copy test disk
		URL url = getClass().getResource("/tinycore-2.1-x86.vdi");
		copyFile(new File(url.getFile()), new File(vmContainer.getMountpoint(),"disk1"));
		//
		return vm;
	}
	
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;
	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        long count = 0;
	        long size = source.size();              
	        while((count += destination.transferFrom(source, 0, size-count))<size);
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}


}
