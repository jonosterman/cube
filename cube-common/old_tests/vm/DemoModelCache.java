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
