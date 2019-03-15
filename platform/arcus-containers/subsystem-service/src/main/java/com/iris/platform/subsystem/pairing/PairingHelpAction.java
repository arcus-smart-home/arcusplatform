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
package com.iris.platform.subsystem.pairing;

import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.iris.messages.type.PairingHelpStep;
import com.iris.prodcat.ProductCatalogEntry;

/**
 * @author tweidlin
 *
 */
public enum PairingHelpAction {
	RangeCheck("rangecheck", "If possible, move the device closer to the Iris hub.") {
		@Override
		public Optional<PairingHelpStep> forProduct(ProductCatalogEntry entry, PairingProtocol protocol) {
			if(protocol.isHub() && protocol.isWirelessMesh()) {
				return super.forProduct(entry, protocol);
			}
			else {
				return Optional.empty();
			}
		}
	},
	BridgeCheck("bridgecheck", "Make sure the bridge is powered and connected to the internet.") {
		@Override
		public Optional<PairingHelpStep> forProduct(ProductCatalogEntry entry, PairingProtocol protocol) {
			if(!StringUtils.isEmpty(entry.getDevRequired())) {
				// FIXME should this insert the name of the device that needs to be powered and connected?
				return super.forProduct(entry, protocol);
			}
			else {
				return Optional.empty();
			}
		}
	},
	EthernetCheck("ethernetcheck", "Ensure the camera's ethernet cord is plugged into your router.") {
		@Override
		public Optional<PairingHelpStep> forProduct(ProductCatalogEntry entry, PairingProtocol protocol) {
			if(protocol.isEthernetPairing()) {
				return super.forProduct(entry, protocol);
			}
			else {
				return Optional.empty();
			}
		}
	},
	WifiCheck("wificheck", "Ensure the device is connected to the internet per the manufacturer's instructions.") {
		@Override
		public Optional<PairingHelpStep> forProduct(ProductCatalogEntry entry, PairingProtocol protocol) {
			if(protocol.isIpcd()) {
				PairingHelpStep step = toStep();
				if(StringUtils.isEmpty(entry.getInstructionsUrl())) {
					step.setAction(PairingHelpStep.ACTION_LINK);
					step.setLinkText("View Instructions");
					step.setLinkUrl(entry.getInstructionsUrl());
				}
				return super.forProduct(entry, protocol);
			}
			else {
				return Optional.empty();
			}
		}
	},
	FormCheck("formcheck", "Confirm the code entered is correct.", PairingHelpStep.ACTION_FORM) {
		@Override
		public Optional<PairingHelpStep> forProduct(ProductCatalogEntry entry, PairingProtocol protocol) {
			// FIXME should really just be checking if there are input parameters...
			if(protocol.isIpcd()) {
				return super.forProduct(entry, protocol);
			}
			else {
				return Optional.empty();
			}
		}
	},
	StepCheck("stepcheck", "Ensure the pairing steps were followed and the device is in pairing mode per the manufacturer's instructions.", PairingHelpStep.ACTION_PAIRING_STEPS),
	FactoryReset("factoryreset", "Factory reset the device.", PairingHelpStep.ACTION_FACTORY_RESET) {
		@Override
		public Optional<PairingHelpStep> forProduct(ProductCatalogEntry entry, PairingProtocol protocol) {
			if(protocol == PairingProtocol.ZWAV || !CollectionUtils.isEmpty(entry.getReset())) {
				return super.forProduct(entry, protocol);
			}
			else {
				return Optional.empty();
			}
		}
	};

	String id;
	String message;
	String action;
	
	PairingHelpAction(String id, String message) {
		this(id, message, PairingHelpStep.ACTION_INFO);
	}
	
	PairingHelpAction(String id, String message, String action) {
		this.id = id;
		this.message = message;
		this.action = action;
	}

	public PairingHelpStep toStep() {
		PairingHelpStep step = new PairingHelpStep();
		step.setId(id);
		step.setMessage(message);
		step.setAction(action);
		return step;
	}
	
	public Optional<PairingHelpStep> forProduct(ProductCatalogEntry entry, PairingProtocol protocol) {
		return Optional.of( toStep() );
	}
	
}

