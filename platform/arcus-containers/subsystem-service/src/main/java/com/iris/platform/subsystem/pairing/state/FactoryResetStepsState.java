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

import java.util.Optional;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemContext.ResponseAction;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.platform.subsystem.pairing.PairingUtils;

public class FactoryResetStepsState extends PairingState {

	FactoryResetStepsState() {
		super(PairingStateName.FactoryResetSteps);
	}

	@Override
	public String onEnter(SubsystemContext<PairingSubsystemModel> context) {
		// TODO consider a dedicated timer for factory reset?
		resetSearchTimeout(context);
		// this may be removed to allow the hub to stay in Zigbee pairing mode during a factory reset
		PairingUtils
			.getHubModel(context)
			.ifPresent((hub) -> {
				if(HubModel.isStatePAIRING(hub)) {
					stopHubPairing(context, hub.getAddress());
				}
			});
		return super.onEnter(context);
	}
	
	@Override
	public void onExit(SubsystemContext<PairingSubsystemModel> context) {
		Optional<Model> hubRef = PairingUtils.getHubModel(context);
		if(hubRef.isPresent() && HubModel.isStateUNPAIRING(hubRef.get())) {
			MessageBody stopUnpairingRequest =
					HubCapability.UnpairingRequestRequest
						.builder()
						.withActionType(HubCapability.UnpairingRequestRequest.ACTIONTYPE_STOP_UNPAIRING)
						.build();
			// logging response handler because the hub will eventually exit this mode of its own accord
			sendHubRequest(context, stopUnpairingRequest, new ResponseAction<PairingSubsystemModel>() {
				
				@Override
				public void onTimeout(SubsystemContext<PairingSubsystemModel> context) {
					context.logger().debug("Request to stop hub unpairing timed out");
				}
				
				@Override
				public void onResponse(SubsystemContext<PairingSubsystemModel> context, PlatformMessage response) {
					context.logger().debug("Request to stop hub unpairing succeeded");
				}
				
				@Override
				public void onError(SubsystemContext<PairingSubsystemModel> context, Throwable cause) {
					context.logger().debug("Request to stop hub unpairing failed", cause);
				}
			});
		}
		super.onExit(context);
	}

	@Override
	public String onTimeout(SubsystemContext<PairingSubsystemModel> context) {
		context.logger().debug("Pairing steps timed out");
		return PairingStateName.Idle.name();
	}

}

