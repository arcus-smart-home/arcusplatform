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
package com.iris.platform.subsystem.pairing.attribute;

import java.util.Date;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.platform.subsystem.pairing.PairingUtils;
import com.iris.platform.subsystem.pairing.ProductLoaderForPairing.ProductCacheInfo;

@Singleton
public class Initializer {
	@Inject private PairingDevicesAttributeBinder pairingDevices;
	
	public void onAdded(SubsystemContext<PairingSubsystemModel> context) {
		context.model().setAvailable(true); // if you have a place, then you can pair something
		context.model().setPairingMode(PairingSubsystemCapability.PAIRINGMODE_IDLE);
		context.model().setPairingModeChanged(new Date());
		context.model().setPairingDevices(ImmutableList.of());
		context.model().setSearchProductAddress("");
		ProductCacheInfo.clear(context);
		context.model().setSearchDeviceFound(false);
		context.model().setSearchIdle(false);
		context.model().setSearchIdleTimeout(PairingUtils.DEFAULT_TIMEOUT);
		context.model().setSearchTimeout(PairingUtils.DEFAULT_TIMEOUT);
	}
	
	public void onStarted(SubsystemContext<PairingSubsystemModel> context) {
		pairingDevices.bind(context);
	}
}

