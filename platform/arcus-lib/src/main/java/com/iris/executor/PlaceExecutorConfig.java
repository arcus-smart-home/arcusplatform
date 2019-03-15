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
package com.iris.executor;

public class PlaceExecutorConfig {
	private int maxThreads = 100;
	private int initialCapacity = 16;
	private int concurrency = Runtime.getRuntime().availableProcessors();
	private int maxQueueDepth = 100;
	
	public int getMaxThreads() {
		return maxThreads;
	}
	
	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}
	
	public int getInitialCapacity() {
		return initialCapacity;
	}
	
	public void setInitialCapacity(int initialCapacity) {
		this.initialCapacity = initialCapacity;
	}
	
	public int getConcurrency() {
		return concurrency;
	}
	
	public void setConcurrency(int concurrency) {
		this.concurrency = concurrency;
	}
	
	public int getMaxQueueDepth() {
		return maxQueueDepth;
	}
	
	public void setMaxQueueDepth(int maxQueueDepth) {
		this.maxQueueDepth = maxQueueDepth;
	}

}

