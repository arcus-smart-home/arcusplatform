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
package com.iris.platform.subsystem.pairing.state;

import java.util.Map;
import java.util.Optional;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.prodcat.ProductCatalogEntry;

public class PairingStepsState extends PairingState {

	PairingStepsState() {
		super(PairingStateName.PairingSteps);
	}

	@Override
	public String onEnter(SubsystemContext<PairingSubsystemModel> context) {
		resetSearchTimeout(context);
		return super.onEnter(context);
	}

	@Override
	public String onTimeout(SubsystemContext<PairingSubsystemModel> context) {
		context.logger().debug("Pairing steps timed out");
		return PairingStateName.Idle.name();
	}

	@Override
	public String search(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request, Optional<ProductCatalogEntry> product, Map<String, Object> form) {
		resetSearchTimeout(context, product);
		return super.search(context, request, product, form);
	}

}

