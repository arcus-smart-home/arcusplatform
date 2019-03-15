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
/**
 * 
 */
package com.iris.platform.model.handler;

import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.PersistentModel;
import com.iris.platform.model.PersistentModelWrapper;

/**
 * @author tweidlin
 *
 */
@Singleton
public class GetAttributesRequestHandler {

	@Request(Capability.CMD_GET_ATTRIBUTES)
	public MessageBody getAttributes(PersistentModelWrapper<? extends PersistentModel> wrapper) {
		// FIXME filter out requested namespaces
		return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, wrapper.model().toMap());
	}
}

