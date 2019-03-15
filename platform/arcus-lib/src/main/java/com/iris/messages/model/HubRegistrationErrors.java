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
package com.iris.messages.model;

public enum HubRegistrationErrors {
	
	VERSION_CAN_NOT_UPGRADE("Hub at firmware version [%s] can't be upgraded"),
	TIMED_OUT("Hub upgrade has exceeded timeout limit"),
	REFUSED("Hub returns REFUSED status when requesting firmware upgrade"),
	VERSION_STILL_BELOW_MINIMUM("Hub registration state is APPLYING, but the hub reports firmware version [%s] still below miniumum");
	
	
	private String msg;
	private HubRegistrationErrors(String msg) {
		this.msg = msg;
	}
	
	public String getMessage(Object ...params) {
		if(params != null && params.length > 0) {
			return String.format(this.msg, params);
		}else {
			return this.msg;
		}
		
	}
	
	public String getCode() {
		return this.name();
	}
	
}

