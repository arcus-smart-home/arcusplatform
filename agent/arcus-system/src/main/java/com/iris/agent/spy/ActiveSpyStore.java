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
package com.iris.agent.spy;

import java.util.Queue;
import java.util.stream.Stream;

import com.iris.agent.util.ConcurrentCappedLinkedQueue;
import com.iris.messages.PlatformMessage;

class ActiveSpyStore implements SpyStore {
	private final static int DEFAULT_PLATFORM_MSG_CAP = 20;
	
	private final Queue<PlatformMessage> outToPlatform;
	private final Queue<PlatformMessage> inFromPlatform;
	
	ActiveSpyStore() {
		final int msgCap = calcPlatformMessageCap();
		outToPlatform = new ConcurrentCappedLinkedQueue<>(msgCap);
		inFromPlatform = new ConcurrentCappedLinkedQueue<>(msgCap);
	}

	@Override
	public void storeIncomingPlatformMsg(PlatformMessage msg) {
		inFromPlatform.add(msg);
	}

	@Override
	public void storeOutgoingPlatformMsg(PlatformMessage msg) {
		outToPlatform.add(msg);
	}

	@Override
	public Stream<PlatformMessage> streamIncomingPlatformMsgs() {
		return inFromPlatform.stream();
	}

	@Override
	public Stream<PlatformMessage> streamOutgoingPlatformMsgs() {
		return outToPlatform.stream();
	}
	
	private static int calcPlatformMessageCap() {
		String capStr = System.getenv("IRIS_HUB_SPY_PLATFORM_MSG_CAP");
		
		if (capStr != null) {
			int cap = DEFAULT_PLATFORM_MSG_CAP; 
			try {
				cap = Integer.valueOf(capStr);
			} 
			catch(Throwable t) {
				// Swallow Exception.
			}
			return cap;
		}
		else {
			return DEFAULT_PLATFORM_MSG_CAP;
		}
	}

}

