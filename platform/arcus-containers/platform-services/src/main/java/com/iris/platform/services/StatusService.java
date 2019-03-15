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
package com.iris.platform.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.services.PlatformConstants;

@Singleton
public class StatusService extends AbstractPlatformService {
	public static final String NAME = PlatformConstants.SERVICE_STATUS;

	@Inject
	public StatusService(PlatformMessageBus platformBus) {
	   super(platformBus, NAME);
	}

	@Override
   public MessageBody handleRequest(MessageBody body) throws Exception {
		if(MessageConstants.MSG_PING_REQUEST.equals(body.getMessageType())) {
		   return ping();
		}
		else {
			return super.handleRequest(body);
		}
	}

	public MessageBody ping() {
	   return MessageBody.pong();
    }
}

