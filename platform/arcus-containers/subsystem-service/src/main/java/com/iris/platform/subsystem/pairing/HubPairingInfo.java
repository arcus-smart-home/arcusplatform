/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.platform.subsystem.pairing;

import com.iris.platform.subsystem.pairing.state.PairingStateName;

public class HubPairingInfo {
	
	private final PairingStateName pending;
	private final String productAddress;
	private final long version;
	
	public HubPairingInfo(
			PairingStateName pending,
			String productAddress,
			long version
	) {
		this.pending = pending;
		this.productAddress = productAddress;
		this.version = version;
	}

	public PairingStateName getPending() {
		return pending;
	}

	public String getProductAddress() {
		return productAddress;
	}

	public long getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return "HubPairingInfo [pending=" + pending + ", productAddress=" + productAddress + ", version=" + version + "]";
	}
	
}

