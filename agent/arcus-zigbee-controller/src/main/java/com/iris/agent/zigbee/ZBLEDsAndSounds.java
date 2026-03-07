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
package com.iris.agent.zigbee;

import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.LEDState;
import com.iris.agent.hal.SounderMode;
import com.iris.agent.zigbee.events.ZBEvent;
import com.iris.agent.zigbee.events.ZBEventDispatcher;
import com.iris.agent.zigbee.events.ZBEventListener;

public class ZBLEDsAndSounds implements ZBEventListener {

   public ZBLEDsAndSounds() {
      ZBEventDispatcher.INSTANCE.register(this);
   }

   @Override
   public void onZBEvent(ZBEvent event) {
      switch (event.getType()) {
         case NODE_ADDED:
            IrisHal.setLedState(LEDState.DEVICE_PAIRED);
            IrisHal.setSounderMode(SounderMode.PAIRED);
            break;
         case NODE_REMOVED:
            IrisHal.setLedState(LEDState.DEVICE_REMOVED);
            IrisHal.setSounderMode(SounderMode.UNPAIRED);
            break;
         default:
            break;
      }
   }
}
