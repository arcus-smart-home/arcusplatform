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
package com.iris.common.subsystem.climate;

import java.util.Map;

import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.SpaceHeaterCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.capability.VentCapability;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.util.IrisCollections.MapBuilder;

public class ClimateFixtures extends ModelFixtures {

   public static DeviceModel createThermostat() {
      Map<String, Object> attributes = buildThermostatAttributes().create();
      SimpleModel model = new SimpleModel(attributes);
      return new DeviceModel(model);
   }

   public static DeviceBuilder buildThermostatAttributes() {
      return 
            ModelFixtures
               .buildDeviceAttributes(ThermostatCapability.NAMESPACE)
               .put(ThermostatCapability.ATTR_ACTIVE, ThermostatCapability.ACTIVE_RUNNING)
               .put(ThermostatCapability.ATTR_AUTOFANSPEED, 1)
               ;
               
   }

   public static DeviceBuilder buildFanAttributes() {
      return 
            ModelFixtures
               .buildDeviceAttributes(FanCapability.NAMESPACE)
               .put(FanCapability.ATTR_DIRECTION, FanCapability.DIRECTION_UP)
               .put(FanCapability.ATTR_MAXSPEED, 10)
               .put(FanCapability.ATTR_SPEED, 2)
               ;
               
   }

   public static DeviceBuilder buildVentAttributes() {
      return 
            ModelFixtures
               .buildDeviceAttributes(VentCapability.NAMESPACE)
               .put(VentCapability.ATTR_VENTSTATE, VentCapability.VENTSTATE_OK)
               .put(VentCapability.ATTR_LEVEL, 75)
               .put(VentCapability.ATTR_AIRPRESSURE, 1.0) // not a realistic value
               ;
               
   }
   
   
   public static DeviceBuilder buildHeaterAttributes(String heaterState) {
	   return 
            ModelFixtures
               .buildDeviceAttributes(SpaceHeaterCapability.NAMESPACE)
               .put(SpaceHeaterCapability.ATTR_HEATSTATE, heaterState)
               .put(SpaceHeaterCapability.ATTR_MAXSETPOINT, 90.0)
               .put(SpaceHeaterCapability.ATTR_MINSETPOINT, 20.0) 
               ;
   }

}

