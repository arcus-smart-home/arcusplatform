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
package com.iris.platform.pairing.customization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.iris.bootstrap.ServiceLocator;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RuleTemplateCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.Place;
import com.iris.messages.type.PairingCustomizationStep;

class RuleRecommendationCustomization extends QueryPairingCustomization {
	private static final Logger logger = LoggerFactory.getLogger(RuleRecommendationCustomization.class);

	private final List<String> templateIds;
	
	public RuleRecommendationCustomization(
			List<String> templateIds,
			Predicate<Model> matcher,
			String action,
			String id,
			@Nullable String header,
			@Nullable String title,
			@Nullable String note,
			@Nullable List<String> description,
			@Nullable String linkText,
			@Nullable String linkUrl
	) {
		super(
				matcher,
				action,
				id,
				header,
				title,
				note,
				description,
				linkText,
				linkUrl
		);
		this.templateIds = ImmutableList.copyOf(templateIds);
	}

	@Override
	public Optional<PairingCustomizationStep> toStepIf(Place place, Model device) {
		if(!apply(place, device)) {
			return Optional.empty();
		}
		
		try {
			Map<String, Boolean> supportedTemplates =
				ServiceLocator
					.getInstance(RuleTemplateRequestor.class)
					.listTemplatesForPlace(place.getId())
					.get()
					.stream()
					.collect(Collectors.toMap((o) -> (String) o.get(Capability.ATTR_ID), (o) -> (Boolean) o.get(RuleTemplateCapability.ATTR_SATISFIABLE)));
			int maxRecommendations = ServiceLocator.getInstance(PairingCustomizationConfig.class).getMaxRuleRecommendations();
			int matchedRecommendations = 0;
			List<String> satisfiableTemplates = new ArrayList<>();
			for(String templateId: templateIds) {
				if(supportedTemplates.getOrDefault(templateId, false)) {
					satisfiableTemplates.add(Address.platformService(templateId, RuleTemplateCapability.NAMESPACE).getRepresentation());
					matchedRecommendations++;
				}
				if(matchedRecommendations >= maxRecommendations) {
					break;
				}
			}
			if(satisfiableTemplates.isEmpty()) {
				logger.debug("None of the recommended templates were satisfiable, so disabling rule customization");
				return Optional.empty();
			}
			else {
				PairingCustomizationStep step = toStep();
				step.setChoices(satisfiableTemplates);
				return Optional.of(step);
			}
		}
		catch (Exception e) {
			logger.debug("Error checking rule templates, not returning this customization", e);
			return Optional.empty();
		}
	}

}

