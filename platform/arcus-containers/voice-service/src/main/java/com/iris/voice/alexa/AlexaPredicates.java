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
package com.iris.voice.alexa;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.DevicePowerModel;
import com.iris.messages.model.serv.SceneModel;
import com.iris.messages.type.Action;
import com.iris.prodcat.ProductCatalogEntry;

public enum AlexaPredicates {
   ;

   private static final String SWITCHES_ACTION = "switches";
   private static final String FANS_ACTION = "fans";
   private static final String BLINDS_ACTION = "blinds";
   private static final String THERMOSTATS_ACTION = "thermostats";
   private static final String VENTS_ACTION = "vents";
   private static final String WATERVALVES_ACTION = "watervalves";
   private static final String SPACEHEATERS_ACTION = "spaceheaters";

   public static boolean supportedDevice(Model m, @Nullable ProductCatalogEntry pm) {
      return m.supports(DeviceCapability.NAMESPACE) && deviceSupported(m, pm);
   }

   public static boolean supportedScene(Model m) {
      return m.supports(SceneCapability.NAMESPACE) && sceneSupported(m);
   }

   public static boolean supported(Model m, @Nullable ProductCatalogEntry pm) {
      return supportedDevice(m, pm) || supportedScene(m);
   }

   public static boolean batteryPowered(Model m) {
      if(m.supports(DevicePowerCapability.NAMESPACE)) {
         return DevicePowerModel.isSourceBATTERY(m);
      }
      return false;
   }

   private static boolean sceneSupported(Model m) {
      if(StringUtils.isBlank(SceneModel.getName(m))) {
         return false;
      }
      List<Action> actions = SceneModel.getActions(m, ImmutableList.of()).stream().map(Action::new).collect(Collectors.toList());
      return
         SceneModel.getEnabled(m) &&
         actions.stream().allMatch(action -> {
            switch(action.getTemplate()) {
               case SWITCHES_ACTION:
               case FANS_ACTION:
               case BLINDS_ACTION:
               case THERMOSTATS_ACTION:
               case VENTS_ACTION:
               case WATERVALVES_ACTION:
               case SPACEHEATERS_ACTION:
                  return true;
               default:
                  return false;
            }
         });
   }

   private static boolean deviceSupported(Model m, @Nullable ProductCatalogEntry pm) {
      if(StringUtils.isBlank(DeviceModel.getName(m))) {
         return false;
      }

      if(pm != null && !pm.getCanDiscover()) {
         return false;
      }

      return
         m.supports(ThermostatCapability.NAMESPACE) ||
         m.supports(SwitchCapability.NAMESPACE) ||
         m.supports(DimmerCapability.NAMESPACE) ||
         m.supports(FanCapability.NAMESPACE) ||
         m.supports(LightCapability.NAMESPACE) ||
         m.supports(DoorLockCapability.NAMESPACE);
   }
}

