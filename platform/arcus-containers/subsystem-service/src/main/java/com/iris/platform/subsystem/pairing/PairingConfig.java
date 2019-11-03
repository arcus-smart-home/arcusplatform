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

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PairingConfig {
	@Inject(optional = true) @Named("pairing.idle.timeoutMs")
	private long idleTimeoutMs = TimeUnit.SECONDS.toMillis(60);
	
	@Inject(optional = true) @Named("pairing.search.timeoutMs")
	private long pairingTimeoutMs = TimeUnit.MINUTES.toMillis(5);
	
	@Inject(optional = true) @Named("pairing.request.timeoutMs")
	private long requestTimeoutMs = TimeUnit.SECONDS.toMillis(30);

	public long getIdleTimeoutMs() {
		return idleTimeoutMs;
	}

	public void setIdleTimeoutMs(long idleTimeoutMs) {
		this.idleTimeoutMs = idleTimeoutMs;
	}

	public long getPairingTimeoutMs() {
		return pairingTimeoutMs;
	}

	public void setPairingTimeoutMs(long pairingTimeoutMs) {
		this.pairingTimeoutMs = pairingTimeoutMs;
	}

	public long getRequestTimeoutMs() {
		return requestTimeoutMs;
	}

	public void setRequestTimeoutMs(long bridgeTimeoutMs) {
		this.requestTimeoutMs = bridgeTimeoutMs;
	}

}

