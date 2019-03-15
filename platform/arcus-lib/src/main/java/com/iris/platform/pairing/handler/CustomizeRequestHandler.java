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
package com.iris.platform.pairing.handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.PairingDeviceCapability.CustomizeRequest;
import com.iris.messages.capability.PairingDeviceCapability.CustomizeResponse;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Place;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.messages.type.PairingCustomizationStep;
import com.iris.platform.model.PersistentModelWrapper;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.customization.PairingCustomizationManager;
import com.iris.validators.ValidationException;

@Singleton
public class CustomizeRequestHandler {
	private final PairingCustomizationManager manager;
	
	@Inject
	public CustomizeRequestHandler(PairingCustomizationManager manager) {
		this.manager = manager;
	}
	
	@Request(CustomizeRequest.NAME)
	public MessageBody customize(Place place, PersistentModelWrapper<PairingDevice> context) throws ValidationException {
		Errors.assertValidRequest(PairingDeviceModel.isPairingStatePAIRED(context.model()), "Can only customize a fully PAIRED device");
		List<Map<String, Object>> steps = 
			manager
				.apply(place, context.model())
				.stream()
				.map(PairingCustomizationStep::toMap)
				.collect(Collectors.toList());
		return 
			CustomizeResponse
				.builder()
				.withSteps(steps)
				.build();
	}
}

