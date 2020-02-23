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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.PlatformBusClient;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.service.PairingDeviceService;
import com.iris.messages.service.RuleService;
import com.iris.messages.service.RuleService.ListRuleTemplatesRequest;
import com.iris.messages.service.RuleService.ListRuleTemplatesResponse;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.IrisUUID;

@Singleton
public class RuleTemplateRequestor {
	public static final String NAME_EXECUTOR = "executor.ruletemplaterequest";
	
	private final PlatformBusClient requestor;
	private final PlacePopulationCacheManager populationCacheMgr;
	
	@Inject
	public RuleTemplateRequestor(
			@Named(NAME_EXECUTOR) Executor executor,
			PlatformMessageBus bus,
			PlacePopulationCacheManager populationCacheMgr,
			PairingCustomizationConfig config
	) {
		this.requestor = new PlatformBusClient(bus, executor, config.getRuleTemplateRequestTimeoutMs(), ImmutableSet.of(AddressMatchers.equals(PairingDeviceService.ADDRESS)));
		this.populationCacheMgr = populationCacheMgr;
	}
	
	public ListenableFuture<List<Map<String, Object>>> listTemplatesForPlace(UUID placeId) {
		MessageBody listTemplates =
			ListRuleTemplatesRequest
				.builder()
				.withPlaceId(placeId.toString())
				.build();
		PlatformMessage request =
			PlatformMessage
				.request(RuleService.ADDRESS)
				.from(PairingDeviceService.ADDRESS)
				.withCorrelationId(IrisUUID.randomUUID().toString())
				.withPlaceId(placeId)
				.withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
				.withPayload(listTemplates)
				.create();
		return 
			Futures.transform(
				requestor.request(request), 
				(Function<PlatformMessage, List<Map<String, Object>>>) (message) -> ListRuleTemplatesResponse.getRuleTemplates(message.getValue(), ImmutableList.of()),
				MoreExecutors.directExecutor()
			);
	}
	
	
}

