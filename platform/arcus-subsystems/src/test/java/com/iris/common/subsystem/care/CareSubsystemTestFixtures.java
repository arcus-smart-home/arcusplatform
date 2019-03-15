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
package com.iris.common.subsystem.care;

import java.util.Map;
import java.util.UUID;

import com.iris.messages.address.Address;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.model.test.ModelFixtures.DeviceBuilder;
import com.iris.util.IrisCollections;

public class CareSubsystemTestFixtures extends ModelFixtures {   
   
   static Map<String, Object> createFixture(String ... namespaces) {	   
      Map<String, Object> attrs = ModelFixtures.buildDeviceAttributes(namespaces).create();
      attrs.put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      return attrs;
   }
   
  public static CareSubsystemModel createCareSubsystemModel() {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, CareSubsystemCapability.NAMESPACE);
      return new CareSubsystemModel(new SimpleModel(attributes));
   }


  static Map<String, Object> createSwitchFixture() {
     return createFixture(SwitchCapability.NAMESPACE);
  }

  
   static Map<String, Object> createMotionFixture() {	   
      return createFixture(MotionCapability.NAMESPACE);
   }

   static Map<String, Object> createContactFixture() {
      return createFixture(ContactCapability.NAMESPACE);
   }

   static Map<String, Object> createPresenceFixture() {
      return createFixture(PresenceCapability.NAMESPACE);
   }

   static Map<String, Object> createKeypadV2Fixture() {
      return createFixture(KeyPadCapability.NAMESPACE,MotionCapability.NAMESPACE);
   }

   
}

