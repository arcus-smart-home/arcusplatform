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
package com.iris.agent.reflex;

import static org.junit.Assert.assertTrue;

import org.easymock.EasyMock;
import org.junit.Test;

import com.iris.agent.router.Router;
import com.iris.agent.zigbee.ZigbeeLocalProcessing;
import com.iris.agent.zwave.ZWaveLocalProcessing;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubReflexCapability;
import com.iris.messages.services.PlatformConstants;

public class ReflexControllerTest {

   // Both platform senders address the hub service, so this controller only
   // sees the event by snooping it.
   @Test
   public void snoopsSyncNeededSentToTheHubService() {
      ReflexController controller = new ReflexController(
         EasyMock.createNiceMock(ReflexLocalProcessing.class),
         EasyMock.createNiceMock(ZigbeeLocalProcessing.class),
         EasyMock.createNiceMock(ZWaveLocalProcessing.class),
         new Router()
      );

      PlatformMessage syncNeeded = PlatformMessage
         .buildEvent(HubReflexCapability.SyncNeededEvent.instance(), Address.platformService(PlatformConstants.SERVICE_DEVICES))
         .to(Address.hubService("TST-0000", PlatformConstants.SERVICE_HUB))
         .create();

      assertTrue(controller.isInterestedIn(syncNeeded));
   }
}
