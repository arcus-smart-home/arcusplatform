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
package com.iris.client.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.event.ClientEvent;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;

public abstract class ErrorEventHandler<E extends ClientEvent> implements ClientEventHandler<E>{
	private Logger logger = LoggerFactory.getLogger(ErrorEventHandler.class);

	@Override
	public void handleMessage(ClientMessage message) {
		ErrorEvent event = (ErrorEvent) message.getPayload();
		logger.error("Received {}, Message: {}, Code: {}", 
				event.getClass().getSimpleName(),
				event.getMessage(),
				event.getCode());

		throw new RuntimeException(event.getMessage());
	}
}

