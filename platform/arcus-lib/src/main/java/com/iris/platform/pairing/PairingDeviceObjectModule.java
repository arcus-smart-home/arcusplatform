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
package com.iris.platform.pairing;

import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.platform.model.ObjectDispatcherModule;
import com.iris.platform.pairing.handler.AddCustomizationRequestHandler;
import com.iris.platform.pairing.handler.CustomizeRequestHandler;
import com.iris.platform.pairing.handler.DismissRequestHandler;
import com.iris.platform.pairing.handler.RemoveRequestHandler;
import com.iris.platform.pairing.handler.UpdatePairingPhaseRequestHandler;
import com.iris.platform.pairing.resolver.PairingDeviceResolver;
import com.netflix.governator.annotations.Modules;

/**
 * @author tweidlin
 *
 */
@Modules(include=PairingDeviceDaoModule.class)
public class PairingDeviceObjectModule extends ObjectDispatcherModule {

	/* (non-Javadoc)
	 * @see com.iris.model.CapabilityDispatcherModule#name()
	 */
	@Override
	protected String name() {
		return PairingDeviceCapability.NAMESPACE;
	}

	@Override
	protected void configure() {
		super.configure();
		
		// resolvers
		addArgumentResolverBinding().to(PairingDeviceResolver.class);
		
		// base request handlers
		bindBaseHandlers();
		
		// pairing device request handlers
		annotatedObjects().addBinding().to(AddCustomizationRequestHandler.class);
		annotatedObjects().addBinding().to(CustomizeRequestHandler.class);
		annotatedObjects().addBinding().to(DismissRequestHandler.class);
		annotatedObjects().addBinding().to(RemoveRequestHandler.class);
		
		// mock pairing device request handlers
		annotatedObjects().addBinding().to(UpdatePairingPhaseRequestHandler.class);
	}

}

