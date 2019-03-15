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
package com.iris.client.impl.netty;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ClientConfig {

	public static final int DFLT_MAX_RECONNECTION_ATTEMPTS = Integer.MAX_VALUE;
	public static final int DFLT_SECS_BETWEEN_RECONNECTION_ATTEMPTS = 5;
	public static final int DFLT_MAX_RESPONSE_SIZE = 1024 * 1024 * 10;
	
	// DEFAULT VALUES ARE NOT SET, THIS FORCES THE USER
	// TO ACTUALLY PICK CORRECT VALUES FOR THEIR PARTICULAR USE CASE
	private int maxReconnectionAttempts;
	private int secondsBetweenReconnectionAttempts;
	private int maxResponseSize;

	private ClientConfig() {
		
	}
	
	public int getMaxReconnectionAttempts() {
		return maxReconnectionAttempts;
	}

	public int getSecondsBetweenReconnectionAttempts() {
		return secondsBetweenReconnectionAttempts;
	}

	public int getMaxResponseSize() {
		return maxResponseSize;
	}

	public static Builder builder() {
		return new Builder();
	}

	public void setConfig(ClientConfig other) {
		if (other != null && !this.equals(other)) {
			synchronized (this) {
				this.maxReconnectionAttempts = other.maxReconnectionAttempts;
				this.secondsBetweenReconnectionAttempts = other.secondsBetweenReconnectionAttempts;
				this.maxResponseSize = other.maxResponseSize;
			}
		}
	}

	public interface Build {
		Build maxReconnectionAttempts(int maxReconnectionAttempts);
		Build secondsBetweenReconnectionAttempts(int secondsBetweenReconnectionAttempts);
		Build maxResponseSize(int maxResponseSize);

		ClientConfig build();
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public static class Builder implements Build {

		// Build new ClientConfig
		private ClientConfig clientConfig = new ClientConfig();

		@Override
		public Build maxReconnectionAttempts(int maxReconnectionAttempts) {
			clientConfig.maxReconnectionAttempts = maxReconnectionAttempts;
			return this;
		}

		@Override
		public Build secondsBetweenReconnectionAttempts(int secondsBetweenReconnectionAttempts) {
			clientConfig.secondsBetweenReconnectionAttempts = secondsBetweenReconnectionAttempts;
			return this;
		}

		@Override
		public Build maxResponseSize(int maxResponseSize) {
			clientConfig.maxResponseSize = maxResponseSize;
			return this;
		}

		@Override
		public ClientConfig build() {
			return clientConfig;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxResponseSize;
		result = prime * result + maxReconnectionAttempts;
		result = prime * result + secondsBetweenReconnectionAttempts;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClientConfig other = (ClientConfig) obj;
		if (maxResponseSize != other.maxResponseSize)
			return false;
		if (maxReconnectionAttempts != other.maxReconnectionAttempts)
			return false;
		if (secondsBetweenReconnectionAttempts != other.secondsBetweenReconnectionAttempts)
			return false;
		return true;
	}
}

