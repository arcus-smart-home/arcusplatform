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

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.HubRegistrationDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.model.HubRegistration;
import com.iris.platform.pairing.PairingDeviceDao;

@Singleton
public class HubDeletedListener extends BaseDeletedListener {
	private static final Logger logger = LoggerFactory.getLogger(HubDeletedListener.class);
	
	private final PairingDeviceDao pairingDeviceDao;
	private final HubRegistrationDAO hubRegistrationDao;
	
	@Inject
	public HubDeletedListener(
			PlatformMessageBus messageBus,
			PairingDeviceDao pairingDeviceDao,
			HubRegistrationDAO hubRegistrationDao
	) {
		super(messageBus, pairingDeviceDao);
		this.pairingDeviceDao = pairingDeviceDao;
		this.hubRegistrationDao = hubRegistrationDao;
	}
	
	// don't use from because hub addresses can be weird
	@OnMessage(types=Capability.EVENT_DELETED)
	public void onHubDeleted(PlatformMessage message) {
		if(!message.getSource().isHubAddress()) {
			return;
		}
		if(StringUtils.isEmpty(message.getPlaceId())) {
			logger.warn("Received hub [{}] deleted with no place id", message.getSource());
			return;
		}
				     
		UUID placeId = UUID.fromString(message.getPlaceId());
		String hubId = message.getSource().getHubId();
		deleteHubRegistrationIfNecessary(hubId); 
		pairingDeviceDao
			.listByPlace(placeId)
			.stream()
			.filter((m) -> StringUtils.equals(hubId, m.getHubId()))
			.forEach(this::delete);
	}
	
	private void deleteHubRegistrationIfNecessary(String hubId) {
		try{
			HubRegistration hubReg = hubRegistrationDao.findById(hubId);
			if(hubReg != null) {
				hubRegistrationDao.delete(hubReg);
			}
		}catch(Exception e) {
			logger.warn("Error deleting from hub_registration table for hub id [{}]", hubId);
		}
		
	}

}

