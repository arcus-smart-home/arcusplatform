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
package com.iris.population;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.core.messaging.MessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.PlaceCapability;

public abstract class AbstractPopulationValueChangeListener implements MessageListener<PlatformMessage> {
	private static final Logger logger = LoggerFactory.getLogger(AbstractPopulationValueChangeListener.class);
	
	protected static final Set<AddressMatcher> SOURCE_ADDRESSES = ImmutableSet.<AddressMatcher>of(AddressMatchers.platformService(MessageConstants.SERVICE, PlaceCapability.NAMESPACE));
	protected final PlacePopulationCacheManager cacheManager;
	protected final PlatformMessageBus msgBus;
	
	@Inject
	public AbstractPopulationValueChangeListener(PlacePopulationCacheManager cacheMgr, PlatformMessageBus msgBus) {
		this.cacheManager = cacheMgr;
		this.msgBus = msgBus;
		msgBus.addBroadcastMessageListener(SOURCE_ADDRESSES, this);
	}
	
	public String getPopulationByPlaceId(UUID placeId) {
		return cacheManager.getPopulationByPlaceId(placeId);		
	}
	
	@Override
	public void onMessage(PlatformMessage message) {
		if(PopulationUtils.isPopulationValueChangeEvent(message)) {
			logger.trace("Population ValueChangeEvent received for place [{}]", message.getSource());
			doOnMessage(message);
      }		
	}

	protected abstract void doOnMessage(PlatformMessage message) ;


}

