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
package com.iris.common.subsystem.doorsnlocks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.type.DoorChimeConfig;

public class TestDoorsNLocksSubsystem_Chime extends DoorsNLocksSubsystemTestCase {

   private Map<String,Object> sensor = DoorsNLocksFixtures.buildContact().create();
   private String sensorAddr;

   private void addSensor() {
      sensorAddr = addModel(sensor).getAddress().getRepresentation();
   }

   @Test
   public void testSyncOnLoadNoConfig() {
      start();
      Set<Map<String,Object>> config = context.model().getChimeConfig();
      assertTrue(config.isEmpty());
   }

   @Test
   public void testSyncOnLoadWithSensor() {
      addSensor();
      start();
      Set<Map<String,Object>> config = context.model().getChimeConfig();
      assertEquals(1, config.size());
      DoorChimeConfig cfg = new DoorChimeConfig(config.iterator().next());
      assertEquals(sensorAddr, cfg.getDevice());
      assertFalse(cfg.getEnabled());
   }

   @Test
   public void testChimeConfigRemoved() {
      addSensor();
      start();
      Set<Map<String,Object>> config = context.model().getChimeConfig();
      assertEquals(1, config.size());
      removeModel(sensorAddr);
      config = context.model().getChimeConfig();
      assertTrue(config.isEmpty());
   }

   @Test
   public void testChimeConfigAdded() {
      start();
      Set<Map<String,Object>> config = context.model().getChimeConfig();
      assertTrue(config.isEmpty());
      addSensor();
      config = context.model().getChimeConfig();
      assertEquals(1, config.size());
   }

   @Test
   public void testChimeSentToAllChimeDevices() {
      addModel(DoorsNLocksFixtures.createHubAttributes());
      addModel(DoorsNLocksFixtures.buildKeypad().create());
      addSensor();
      start();

      Set<Map<String,Object>> config = new HashSet<>(context.model().getChimeConfig());
      for(Map<String,Object> cfg : config) {
         cfg.put(DoorChimeConfig.ATTR_ENABLED, Boolean.TRUE);
      }
      context.model().setChimeConfig(config);
      Map<String,Object> update = ImmutableMap.<String,Object>of(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED);
      updateModel(sensorAddr, update);
      assertSensorOpened(sensorAddr);
      List<MessageBody> bodies = requests.getValues();
      assertEquals(2, bodies.size());
   }
}

