package ch.admin.vbs.cube.dataowner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prototype Application to manage VM
 */
public class DataOwner {
	private static final Logger LOG = LoggerFactory.getLogger(DataOwner.class);
	private File baseDir;
	private File outDir;
	
	public DataOwner() {
		baseDir = new File(new File(System.getProperty("user.home")),"cubedataowner");
		outDir = new File(baseDir, "output");
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		if (!baseDir.exists()) {
			baseDir.mkdirs();
			// make example:
			createExampleStructure();
			
		}
//		disksDir = new File(baseDir,"virtual_disks");
		
		
	}
	
	

	private void createExampleStructure() throws IOException {
		File exDir = new File(baseDir,"example");
		exDir.mkdirs();
		System.out.println("Create example VM in : "+exDir.getAbsolutePath());
		// disk
		File vdi = new File(exDir,"my_disk.vdi");
		FileOutputStream fos = new FileOutputStream(vdi);
		for (int i=0;i<1024*1024*100;i++) {fos.write(0);}
		fos.close();
		// config
		File cfg = new File(exDir,"vm.cfg");
		BufferedWriter bw = new BufferedWriter(new FileWriter(cfg));
		
		// user keys
		File key1 = new File(exDir,"user1.pem");
		File key2 = new File(exDir,"user2.pem");
	}



	public static void main(String[] args) {
		new DataOwner();
	}
}
