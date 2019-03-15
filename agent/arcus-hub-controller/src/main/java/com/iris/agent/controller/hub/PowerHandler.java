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
package com.iris.agent.controller.hub;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.fourg.FourgService;
import com.iris.agent.hal.BatteryStateListener;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubPowerCapability;
import com.iris.protocol.ProtocolMessage;

enum PowerHandler implements PortHandler, BatteryStateListener {
   INSTANCE;

   @Nullable
   private HubController controller;

   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Boolean> mains = HubAttributesService.ephemeral(Boolean.class, HubPowerCapability.ATTR_MAINSCPABLE, true);
   private static final HubAttributesService.Attribute<String> source = HubAttributesService.persisted(String.class, HubPowerCapability.ATTR_SOURCE, IrisHal.getPowerSource());
   private static final HubAttributesService.Attribute<Integer> level = HubAttributesService.ephemeral(Integer.class, HubPowerCapability.ATTR_BATTERY, IrisHal.getBatteryLevel());

   void start(HubController controller, Port parent) {
      this.controller = controller;
      IrisHal.addBatteryStateListener(this);
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      return null;
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
   }

   @Override
   public void recv(Port port, Object message) {
   }

   @Override
   public void batteryStateChanged(boolean oldState, boolean newState) {
      source.set(IrisHal.getPowerSource());
      if (controller != null) {
         controller.updateLEDState(newState, LifeCycleService.getState(), FourgService.getState());
      }
   }

   @Override
   public void batteryVoltageChanged(double oldVoltage, double newVoltage) {
      // ignore
   }

   @Override
   public void batteryLevelChanged(double oldLevel, double newLevel) {
      level.set((int)newLevel);
   }
}

