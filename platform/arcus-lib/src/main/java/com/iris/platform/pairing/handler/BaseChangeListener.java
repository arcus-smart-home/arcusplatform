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

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.population.PlacePopulationCacheManager;

public class BaseChangeListener {
	private static final Logger logger = LoggerFactory.getLogger(BaseChangeListener.class);
	private final PlatformMessageBus messageBus;
	private final PairingDeviceDao pairingDeviceDao;
	
	protected BaseChangeListener(PlatformMessageBus messageBus, PairingDeviceDao pairingDeviceDao) {
		this.messageBus = messageBus;
		this.pairingDeviceDao = pairingDeviceDao;
	}
	
	protected PairingDevice updatePairingStatus(PairingDevice model,
	      MessageBody message,
	      @Nullable Map<String, Object> original) {
	   return updatePairingStatus(model, message, original, PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceCapability.PAIRINGPHASE_PAIRED);
	}
	
	protected PairingDevice updatePairingStatus(PairingDevice model, 
	      MessageBody message, 
	      @Nullable Map<String, Object> original, 
	      String pairingState, 
	      String pairingPhase) {		
		if(isPairingFinal(model)) {
			return model;
		}
		
		Map<String, String> errors = DeviceAdvancedCapability.getErrors(message, ImmutableMap.of());
		if(errors.containsKey(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED)) {
			PairingDeviceModel.setPairingState(model, PairingDeviceCapability.PAIRINGSTATE_MISPAIRED);
			PairingDeviceModel.setPairingPhase(model, PairingDeviceCapability.PAIRINGPHASE_FAILED);
		}
		else if(errors.containsKey(DeviceAdvancedCapability.PairingMisconfiguredException.CODE_PAIRING_MISCONFIGURED)) {
			PairingDeviceModel.setPairingState(model, PairingDeviceCapability.PAIRINGSTATE_MISCONFIGURED);
			PairingDeviceModel.setPairingPhase(model, PairingDeviceCapability.PAIRINGPHASE_FAILED);
		}
		else if(DeviceAdvancedCapability.DRIVERSTATE_PROVISIONING.equals(DeviceAdvancedCapability.getDriverstate(message, ""))) {
			PairingDeviceModel.setPairingState(model, PairingDeviceCapability.PAIRINGSTATE_PAIRING);
			PairingDeviceModel.setPairingPhase(model, PairingDeviceCapability.PAIRINGPHASE_CONFIGURE);
		}
		else {
			PairingDeviceModel.setPairingState(model, pairingState);
			PairingDeviceModel.setPairingPhase(model, pairingPhase);
		}
		model = pairingDeviceDao.save(model);
		sendChanges(model, original);
		return model;
	}
	
	//Determine if the current device pairing has reached the ending state, currently defined as 
	//[pairingState, pairingPhase] = [PAIRED, PAIRED] or [MISPAIRED, FAILED]
	private boolean isPairingFinal(PairingDevice model) {
		String currentPairingPhase = PairingDeviceModel.getPairingPhase(model);
		String currentPairingState = PairingDeviceModel.getPairingState(model);
		if(PairingDeviceCapability.PAIRINGPHASE_PAIRED.equals(currentPairingPhase) && PairingDeviceCapability.PAIRINGSTATE_PAIRED.equals(currentPairingState)) {
			logger.warn("Ignoring the incoming pairing status update because of the current pairing phase value [{},{}]", currentPairingPhase, currentPairingState);
			return true;
		}else if(PairingDeviceCapability.PAIRINGPHASE_FAILED.equals(currentPairingPhase) && PairingDeviceCapability.PAIRINGSTATE_MISPAIRED.equals(currentPairingState)) {
			logger.warn("Ignoring the incoming pairing status update because of the current pairing phase value [{},{}]", currentPairingPhase, currentPairingState);
			return true;
		}else{
			return false;
		}
	}

	private void sendChanges(PairingDevice model, Map<String, Object> original) {
		Map<String, Object> attributes = model.toMap();
		if(original != null) {
			Iterator<Map.Entry<String, Object>> attributeIt = attributes.entrySet().iterator();
			while(attributeIt.hasNext()) {
				Map.Entry<String, Object> entry = attributeIt.next();
				if(Objects.equals(entry.getValue(), original.get(entry.getKey()))) {
					attributeIt.remove();
				}
			}
		}
		if(attributes.isEmpty()) {
			// no change, bail
			return;
		}
		PlatformMessage added =
				PlatformMessage
					.builder()
					.from(model.getAddress())
					.withPlaceId(model.getPlaceId())
					.withPopulation(model.getPopulation())
					.withPayload(original == null ? Capability.EVENT_ADDED : Capability.EVENT_VALUE_CHANGE, attributes)
					.create();
		messageBus.send(added);
	}

}

