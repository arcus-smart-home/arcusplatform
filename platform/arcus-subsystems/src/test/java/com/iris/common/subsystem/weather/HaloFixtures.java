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
package com.iris.common.subsystem.weather;

import java.util.Map;
import java.util.UUID;

import com.google.common.base.Optional;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HaloCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.capability.WeatherRadioCapability;
import com.iris.util.IrisCollections;
import com.iris.util.IrisCollections.MapBuilder;

public class HaloFixtures {

   static Map<String, Object> createAlertingOnWeatherHaloPlusFixture() {
      UUID id = UUID.randomUUID();
      return IrisCollections
      .<String, Object>map()
      .put(Capability.ATTR_ID, id.toString())
      .put(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE)
      .put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation())
      .put(Capability.ATTR_CAPS, IrisCollections.setOf(Capability.NAMESPACE,ContactCapability.NAMESPACE))
      .put(HaloCapability.ATTR_DEVICESTATE, HaloCapability.DEVICESTATE_WEATHER)
      .put(HaloCapability.ATTR_HUSHSTATUS, HaloCapability.HUSHSTATUS_READY)
      .put(HaloCapability.ATTR_HALOALERTSTATE, HaloCapability.HALOALERTSTATE_QUIET)
      .put(WeatherRadioCapability.ATTR_PLAYINGSTATE, WeatherRadioCapability.PLAYINGSTATE_PLAYING)
      .put(WeatherRadioCapability.ATTR_ALERTSTATE, WeatherRadioCapability.ALERTSTATE_ALERT).create();
   }
   
   public static Map<String, Object> createPreSmokeOnHaloFixture() {
      UUID id = UUID.randomUUID();
      return IrisCollections
      .<String, Object>map()
      .put(Capability.ATTR_ID, id.toString())
      .put(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE)
      .put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation())
      .put(Capability.ATTR_CAPS, IrisCollections.setOf(Capability.NAMESPACE,SmokeCapability.NAMESPACE))
      .put(HaloCapability.ATTR_DEVICESTATE, HaloCapability.DEVICESTATE_PRE_SMOKE).create();
   }   
   
   public static Map<String, Object> createHaloFixtureWithAttrs(Optional<Map<String, Object>> additionalAttributes) {
      UUID id = UUID.randomUUID();
      MapBuilder<String, Object> builder = IrisCollections
      .<String, Object>map()
      .put(Capability.ATTR_ID, id.toString())
      .put(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE)
      .put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation())
      .put(Capability.ATTR_CAPS, IrisCollections.setOf(Capability.NAMESPACE,SmokeCapability.NAMESPACE))
      .put(HaloCapability.ATTR_DEVICESTATE, HaloCapability.DEVICESTATE_SAFE);
       
      if (additionalAttributes.isPresent()){
         builder.putAll(additionalAttributes.get());
      }

       return builder.create();
   }     
}

