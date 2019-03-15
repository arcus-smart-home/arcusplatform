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
package com.iris.platform.subsystem.pairing.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.capability.PairingSubsystemCapability.ListHelpStepsResponse;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.messages.type.PairingHelpStep;
import com.iris.platform.subsystem.pairing.PairingHelpAction;
import com.iris.platform.subsystem.pairing.PairingProtocol;
import com.iris.platform.subsystem.pairing.ProductLoaderForPairing;
import com.iris.prodcat.ProductCatalogEntry;

@Singleton
public class ListHelpStepsHandler {
	private static final Set<String> VALID_HELP_STATES = ImmutableSet.of(
			PairingSubsystemCapability.PAIRINGMODE_CLOUD,
			PairingSubsystemCapability.PAIRINGMODE_HUB,
			PairingSubsystemCapability.PAIRINGMODE_OAUTH
	);
	private static final List<PairingHelpStep> GenericHubSteps = ImmutableList.of(
			PairingHelpAction.RangeCheck.toStep(), 
			PairingHelpAction.StepCheck.toStep()
	);
	
	private final ProductLoaderForPairing productLoader;
	
	@Inject
	public ListHelpStepsHandler(ProductLoaderForPairing productLoader) {
		this.productLoader = productLoader;
	}
	
	public MessageBody listHelpSteps(SubsystemContext<PairingSubsystemModel> context) {
		if(!VALID_HELP_STATES.contains(context.model().getPairingMode())) {
			throw new PairingSubsystemCapability.RequestStateInvalidException("Can only request help steps when pairingMode is in " + VALID_HELP_STATES);
		}
		List<PairingHelpStep> steps;
		Optional<ProductCatalogEntry> pairingProduct = productLoader.getCurrent(context);
		if(pairingProduct.isPresent()) {
			steps = loadStepsByProduct(pairingProduct.get());
		}
		else {
			steps = GenericHubSteps;
		}
		List<Map<String, Object>> stepList = toMapWithOrder(steps);
		return
			ListHelpStepsResponse
				.builder()
				.withSteps(stepList)
				.build();
	}

	//Add order field PairingHelpStep and convert each PairingHelpStep to a Map
	private List<Map<String, Object>> toMapWithOrder(List<PairingHelpStep> steps) {
		int order = 1;
		List<Map<String, Object>> resultingSteps = new ArrayList<Map<String, Object>>();
		for(PairingHelpStep curStep : steps) {
			curStep.setOrder(order++);
			resultingSteps.add(curStep.toMap());
		}
		return resultingSteps;
	}

	private List<PairingHelpStep> loadStepsByProduct(ProductCatalogEntry product) {
		PairingProtocol protocol = PairingProtocol.forProduct(product);
		List<PairingHelpStep> steps = new ArrayList<>();
		for(PairingHelpAction action: PairingHelpAction.values()) {
			action.forProduct(product, protocol).ifPresent(steps::add);
		}
		return steps;
	}

}

