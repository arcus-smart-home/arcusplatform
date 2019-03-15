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
package com.iris.common.subsystem.cameras;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.capability.PlaceCapability;

public class TestCamerasSubsystem_CanRecord extends CamerasSubsystemTestCase {

   private void addAll(String serviceLevel) {
      Map<String,Object> update = new HashMap<>(place);
      update.put(PlaceCapability.ATTR_SERVICELEVEL, serviceLevel);

      updateModel("SERV:" + PlaceCapability.NAMESPACE + ":" + placeId, update);
   }

   @Test
   public void testSyncOnLoadPremium() {
      addAll(PlaceCapability.SERVICELEVEL_PREMIUM);
      start();
      assertCanRecord();
   }

   @Test
   public void testSyncOnLoadBasic() {
      addAll(PlaceCapability.SERVICELEVEL_BASIC);
      start();
      assertCanRecord();
   }

   @Test
   public void testServiceLevelChange() {
      addAll(PlaceCapability.SERVICELEVEL_PREMIUM);
      start();
      assertCanRecord();

      String addr = "SERV:" + PlaceCapability.NAMESPACE + ":" + placeId;

      Map<String,Object> update = ImmutableMap.<String, Object>of(PlaceCapability.ATTR_SERVICELEVEL, PlaceCapability.SERVICELEVEL_BASIC);
      updateModel(addr, update);
      assertCanRecord();

      update = ImmutableMap.<String, Object>of(PlaceCapability.ATTR_SERVICELEVEL, PlaceCapability.SERVICELEVEL_PREMIUM);
      updateModel(addr, update);
      assertCanRecord();
  }
}

