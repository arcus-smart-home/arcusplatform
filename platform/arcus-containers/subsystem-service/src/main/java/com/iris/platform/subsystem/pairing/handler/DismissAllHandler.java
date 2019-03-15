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

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PairingDeviceCapability.DismissRequest;
import com.iris.messages.capability.PairingSubsystemCapability.DismissAllRequest;
import com.iris.messages.capability.PairingSubsystemCapability.DismissAllResponse;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.messages.type.PairingCompletionStep;
import com.iris.platform.subsystem.pairing.PairingUtils;
import com.iris.platform.subsystem.pairing.state.PairingStateMachine;

@Singleton
public class DismissAllHandler {

	
	@Request(DismissAllRequest.NAME)
	public MessageBody dismissAll(SubsystemContext<PairingSubsystemModel> context) {
		PairingStateMachine.get(context).stopPairing();
		for(String pairingDeviceAddress: context.model().getPairingDevices()) {
			context.request(Address.fromString(pairingDeviceAddress), DismissRequest.instance());
		}
		List<Map<String, Object>> postPairingActions = createPostPairingActions(context);
		// NOTE this can "linger" if DismissAll isn't invoked, due to app crash or timeout
		//      however, since the customization process returns the whole pairing subsystem
		//      to IDLE there isn't a great hook for it, other than here
		//      additionally its probably better to err on the side of showing this too often
		//      rather than not at all
		PairingUtils.clearZWaveRebuildRequired(context);
		return 
			DismissAllResponse
				.builder()
				.withActions(postPairingActions)
				.build();
	}

	private List<Map<String, Object>> createPostPairingActions(SubsystemContext<PairingSubsystemModel> context) {
		// TODO should this use the hubzwave:healRecommended attribute instead?
		if(PairingUtils.isZWaveRebuildRequired(context)) {
			PairingCompletionStep pcs = new PairingCompletionStep();
			pcs.setId("postpairing/" + PairingCompletionStep.ACTION_ZWAVE_REBUILD.toLowerCase());
			pcs.setAction(PairingCompletionStep.ACTION_ZWAVE_REBUILD);
			// TODO need copy to fill this object out more
			return ImmutableList.of(pcs.toMap());
		}
		else {
			return ImmutableList.of();
		}
	}
}

