/**
 * Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package ch.admin.vbs.cube.core.transfer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import ch.admin.vbs.cube.common.RelativeFile;
import ch.admin.vbs.cube.core.vm.IVmModelChangeListener;
import ch.admin.vbs.cube.core.vm.Vm;
import ch.admin.vbs.cube.core.vm.VmModel;

/**
 * Monitors the export directories of all virtual machines.
 */
public class TransferMonitor implements IVmModelChangeListener {
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(TransferMonitor.class);
	private static final int INSPECTION_DELAY = 2000;
	private static List<Vm> userVmList;
	private boolean running;
	private VmModel model;

	/**
	 * Creates a {@link TransferMonitor}.
	 * 
	 * @param transferListener
	 *            the listener listening for files being exported.
	 * @param session
	 */
	public TransferMonitor(final TransferListener transferListener) {
		userVmList = new ArrayList<Vm>();
		// VM monitoring is performed in a separated thread.
		Thread monitorThread = new Thread(new Runnable() {
			@Override
			public void run() {
				running = true;
				Map<String, Map<String, Long>> actualMapsByVm = new HashMap<String, Map<String, Long>>();
				Map<String, Map<String, Long>> formerMapsByVm = new HashMap<String, Map<String, Long>>();
				Map<String, Map<String, Long>> formerFormerMapsByVm = new HashMap<String, Map<String, Long>>();
				while (running) {
					try {
						Thread.sleep(INSPECTION_DELAY);
					} catch (InterruptedException e) {
						LOG.error("Problem during sleep.", e);
					}
					try {
						LOG.debug("Inspect export folders..");
						ArrayList<Vm> clone = null;
						synchronized (userVmList) {
							clone = new ArrayList<Vm>(userVmList);							
						}
						for (Vm vm : clone) {
							File exportFolder = vm.getExportFolder();
							// Do something only if the export folder has been
							// correctly created.
							String vmId = vm.getId();
							if (exportFolder == null) {
								LOG.debug("Export folder is null for VM [{}]", vmId);
							} else if (exportFolder.exists()) {
								// Create all needed maps.
								if (!actualMapsByVm.containsKey(vmId)) {
									actualMapsByVm.put(vmId, new HashMap<String, Long>());
								}
								if (!formerMapsByVm.containsKey(vmId)) {
									formerMapsByVm.put(vmId, new HashMap<String, Long>());
								}
								if (!formerFormerMapsByVm.containsKey(vmId)) {
									formerFormerMapsByVm.put(vmId, new HashMap<String, Long>());
								}
								// Initialize programmer friendlier references.
								Map<String, Long> actualFiles = actualMapsByVm.get(vmId);
								Map<String, Long> formerFiles = formerMapsByVm.get(vmId);
								Map<String, Long> formerFormerFiles = formerFormerMapsByVm.get(vmId);
								// Fill the map with the lastModified times for
								// the
								// actual files.
								for (File file : exportFolder.listFiles()) {
									// Ignore hidden files.
									if (!file.getName().startsWith(".")) {
										actualFiles.put(file.getName(), retrieveLastModified(file));
									}
								}
								// Go through the actual files.
								for (String filename : actualFiles.keySet()) {
									// Take into account only files that already
									// existed in the former iteration and that
									// and that have not changed since (this
									// excludes big fat files being copied whose
									// last modified time changes for each
									// iteration.)
									if (formerFiles.containsKey(filename) && formerFiles.get(filename).longValue() == actualFiles.get(filename).longValue()) {
										// Take into account for a notification
										// files that either did not exist in
										// the
										// before last iteration or whose last
										// modified time has changed
										// between the before last and the last
										// iteration (because they were still
										// being
										// copied.)
										if (!formerFormerFiles.containsKey(filename) || formerFormerFiles.containsKey(filename)
												&& formerFormerFiles.get(filename).longValue() != formerFiles.get(filename).longValue()) {
											LOG.debug("File found [{}] in VM [{}]", new File(exportFolder, filename).getAbsolutePath(), vm.getId());
											transferListener.notifyFileExport(vm, new RelativeFile(new File(exportFolder, filename), exportFolder));
										}
									}
								}
								// Update all the maps.
								formerFormerFiles.clear();
								for (String key : formerFiles.keySet()) {
									formerFormerFiles.put(key, formerFiles.get(key));
								}
								formerFiles.clear();
								for (Entry<String, Long> e : actualFiles.entrySet()) {
									formerFiles.put(e.getKey(), e.getValue());
								}
								actualFiles.clear();
							}
						}
					} catch (Exception e) {
						LOG.error("Monitor fails.", e);
					}
				}
			}
		});
		monitorThread.setDaemon(true);
		monitorThread.start();
	}

	/**
	 * @param file
	 *            the file or directory whose last modified should be retrieved.
	 * @return A long value representing the time the file was last modified.
	 */
	private long retrieveLastModified(File file) {
		if (file.isFile()) {
			return file.lastModified();
		} else {
			long maxValue = 0;
			for (File child : file.listFiles()) {
				maxValue = Math.max(maxValue, retrieveLastModified(child));
			}
			return maxValue;
		}
	}

	public void stop() {
		running = false;
	}

	public void setVmModel(VmModel model) {
		this.model = model;
	}

	@Override
	public void listUpdated() {
		synchronized (userVmList) {
			userVmList.clear();
			userVmList.addAll(model.getVmList());
		}
	}

	@Override
	public void vmUpdated(Vm vm) {
		// not used
	}
}
