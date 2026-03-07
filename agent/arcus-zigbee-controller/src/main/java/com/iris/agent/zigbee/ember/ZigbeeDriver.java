/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.ember;

import com.zsmartsystems.zigbee.ZigBeeCommand;
import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.ZigBeeNode;

public interface ZigbeeDriver {

   void initialize(ZBNetworkCallbacks callbacks);

   void shutdown();

   void permitJoin(int durationInSeconds);

   void denyJoin();

   void leave(long ieeeAddr);

   void formNetwork();

   void send(ZigBeeCommand command);

   void sendApsFrame(com.zsmartsystems.zigbee.aps.ZigBeeApsFrame apsFrame);

   ZigBeeNetworkManager getNetworkManager();

   long getCoordinatorEui64();

   interface ZBNetworkCallbacks {
      void onNodeAdded(ZigBeeNode node);

      void onNodeRemoved(ZigBeeNode node);

      void onNodeUpdated(ZigBeeNode node);

      void onCommandReceived(ZigBeeCommand command);

      void onAnnounce(int nwkAddr, long ieeeAddr);
   }
}
