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
package com.iris.alexa.shs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.alexa.AlexaInterfaces;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.type.AlexaEndpoint;
import com.iris.util.IrisUUID;

public enum ShsFixtures {
   ;

   public static final AlexaEndpoint colorBulb = createEndpoint(
      ImmutableMap.of(DeviceCapability.ATTR_DEVTYPEHINT, "Light"),
      true,
      ImmutableList.of(
         AlexaInterfaces.EndpointHealth.createCapability(false).toMap(),
         AlexaInterfaces.PowerController.createCapability(false).toMap(),
         AlexaInterfaces.ColorTemperatureController.createCapability(false).toMap(),
         AlexaInterfaces.ColorController.createCapability(false).toMap(),
         AlexaInterfaces.BrightnessController.createCapability(false).toMap()
      ),
      ImmutableSet.of("LIGHT"),
      "light"
   );

   public static final AlexaEndpoint thermostat = createEndpoint(
      ImmutableMap.of(DeviceCapability.ATTR_DEVTYPEHINT, "Thermostat"),
      true,
      ImmutableList.of(
         AlexaInterfaces.EndpointHealth.createCapability(false).toMap(),
         AlexaInterfaces.TemperatureSensor.createCapability(false).toMap(),
         AlexaInterfaces.ThermostatController.createCapability(false).toMap()
      ),
      ImmutableSet.of("THERMOSTAT"),
      "thermostat"
   );

   public static final AlexaEndpoint fan = createEndpoint(
      ImmutableMap.of(DeviceCapability.ATTR_DEVTYPEHINT, "Fan Control", FanCapability.ATTR_MAXSPEED, "3"),
      false,
      ImmutableList.of(
         AlexaInterfaces.EndpointHealth.createCapability(false).toMap(),
         AlexaInterfaces.PowerController.createCapability(false).toMap(),
         AlexaInterfaces.PercentageController.createCapability(false).toMap()
      ),
      ImmutableSet.of("SWITCH"),
      "fan"
   );

   public static final AlexaEndpoint scene = createEndpoint(
      ImmutableMap.of(),
      true,
      ImmutableList.of(
         AlexaInterfaces.EndpointHealth.createCapability(false).toMap(),
         AlexaInterfaces.SceneController.CAPABILITY.toMap()
      ),
      ImmutableSet.of("SCENE_TRIGGER"),
      "scene"
   );

   public static final AlexaEndpoint lock = createEndpoint(
      ImmutableMap.of(DeviceCapability.ATTR_DEVTYPEHINT, "Lock"),
      true,
      ImmutableList.of(
         AlexaInterfaces.EndpointHealth.createCapability(false).toMap(),
         AlexaInterfaces.LockController.createCapability(false).toMap()
      ),
      ImmutableSet.of("SMARTLOCK"),
      "lock"
   );

   private static AlexaEndpoint createEndpoint(
      Map<String, String> cookie,
      boolean online,
      List<Map<String, Object>> caps,
      Set<String> categories,
      String name
   ) {
      AlexaEndpoint endpoint = new AlexaEndpoint();
      endpoint.setCookie(cookie);
      endpoint.setModel("Model");
      endpoint.setOnline(online);
      endpoint.setCapabilities(caps);
      endpoint.setDisplayCategories(categories);
      endpoint.setManufacturerName("Manufacturer");
      endpoint.setDescription("Manufacturer Model connected via Iris by Lowe's");
      endpoint.setFriendlyName(name);
      endpoint.setEndpointId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());
      return endpoint;
   }
}

