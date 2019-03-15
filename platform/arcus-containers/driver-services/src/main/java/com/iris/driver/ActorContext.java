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
package com.iris.driver;

import java.util.Date;

import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

public class ActorContext {

	private Address actorAddress;
	private Date requestTime;
	
	public ActorContext(PlatformMessage message) {
		this.actorAddress = message.getActor();
		this.requestTime = new Date();
	}
	
	public Address getActorAddress() {
		return actorAddress;
	}
	public void setActorAddress(Address actorAddress) {
		this.actorAddress = actorAddress;
	}
	public Date getRequestTime() {
		return requestTime;
	}
	public void setRequestTime(Date requestTime) {
		this.requestTime = requestTime;
	}
	
	
}

