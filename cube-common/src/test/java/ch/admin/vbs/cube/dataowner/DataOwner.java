package ch.admin.vbs.cube.dataowner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.channels.FileChannel;

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
		baseDir = new File(new File(System.getProperty("user.home")), "cubedataowner");
		outDir = new File(baseDir, "output");
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		if (!baseDir.exists()) {
			baseDir.mkdirs();
			// make example:
			createExampleStructure();
		}
		// disksDir = new File(baseDir,"virtual_disks");
	}

	private void createExampleStructure() {
		try {
			File exDir = new File(baseDir, "example");
			exDir.mkdirs();
			System.out.println("Create example VM in : " + exDir.getAbsolutePath());
			// disk
			File vdi = new File(exDir, "my_disk.vdi");
			FileOutputStream fos = new FileOutputStream(vdi);
			for (int i = 0; i < 1024 * 1024 * 100; i++) {
				fos.write(0);
			}
			fos.close();
			// config
			File cfg = new File(exDir, "vm.cfg");
			BufferedWriter bw = new BufferedWriter(new FileWriter(cfg));
			bw.write("## Cube example VM config File\n");
			bw.write("name=Example VM\n");
			bw.write("domain=test\n");
			bw.write("description=Example VM in test domain\n");
			bw.write("disks=my_disk.vdi\n");
			bw.write("vpn.remote=vpn.example.net:12345\n");
			bw.write("vpn.key=client.key\n");
			bw.write("vpn.cert=client.crt\n");
			bw.write("vpn.ca=ca.crt\n");
			
			bw.close();
			// user keys
			File file = new File( getClass().getResource("/cube-01_pwd-is-111222.p12").getFile());
			FileChannel in = new FileInputStream(file).getChannel();
			FileChannel out = new FileOutputStream(new File(exDir, "user1.pem")).getChannel();
			in.transferTo(0, in.size(), out);
			in.close();
			out.close();			
			in = new FileInputStream(file).getChannel();
			out = new FileOutputStream(new File(exDir, "user2.pem")).getChannel();
			in.transferTo(0, in.size(), out);
			in.close();
			out.close();			
			
		} catch (Exception e) {
			LOG.error("Failed to setup example.", e);
		}
	}

	public static void main(String[] args) {
		new DataOwner();
	}
}
