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

package ch.admin.vbs.cube.core.usb;

public class UsbDevice {
	private final String productId;
	private final String vendorId;
	private final String description;

	public UsbDevice(String vendorId, String productId, String Description) {
		this.vendorId = vendorId;
		this.productId = productId;
		this.description = Description;
	}

	public String getProductId() {
		return productId;
	}

	public String getVendorId() {
		return vendorId;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return String.format("[%s:%s] %s", vendorId, productId, description);
	}
}
