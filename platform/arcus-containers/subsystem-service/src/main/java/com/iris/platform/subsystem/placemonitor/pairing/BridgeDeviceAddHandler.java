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
package com.iris.platform.subsystem.placemonitor.pairing;

import java.util.concurrent.TimeUnit;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.BridgeCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.platform.subsystem.placemonitor.BasePlaceMonitorHandler;

public class BridgeDeviceAddHandler extends BasePlaceMonitorHandler {

   private static final long DEFAULT_BRIDGE_PAIR_TIMEOUT = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

   @Override
   public void onDeviceAdded(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      if(model.getCapabilities().contains(BridgeCapability.NAMESPACE)) {
         MessageBody body = BridgeCapability.StartPairingRequest.builder().withTimeout(DEFAULT_BRIDGE_PAIR_TIMEOUT).build();
         context.request(model.getAddress(), body);
      }
   }

}

