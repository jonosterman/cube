package ch.admin.vbs.cube.core.vm;

import java.io.File;

import ch.admin.vbs.cube.core.vm.list.DescriptorModelCache;

public class DemoModelCache { 
	public static void main(String[] args) throws Exception {
		DemoModelCache d = new DemoModelCache();
		d.start();
	}

	private void start() throws Exception {
		VmModel model = new VmModel();
		File initial = new File(getClass().getClassLoader().getResource("descriptors.properties").getFile());
		File cfile = new File("/tmp/DemoModelCache.properties");
		Process p = Runtime.getRuntime().exec(new String[]{"cp","-f",initial.getAbsolutePath(), cfile.getAbsolutePath()}  );
		p.waitFor();
		DescriptorModelCache c = new DescriptorModelCache(model, cfile);
		c.start();
		//
		model.getVmList().get(0).getDescriptor().getLocalCfg().setPropertie("time", System.currentTimeMillis()+"");
		model.getVmList().get(0).getDescriptor().getLocalCfg().setPropertie("chars", "@@@@@@@");
		c.listUpdated();
		// reload
		model = new VmModel();
		c = new DescriptorModelCache(model, cfile);
		c.start();
		
		
		
	}
}
