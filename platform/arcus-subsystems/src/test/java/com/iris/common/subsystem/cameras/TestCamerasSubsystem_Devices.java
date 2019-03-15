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

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.capability.DeviceConnectionCapability;

public class TestCamerasSubsystem_Devices extends CamerasSubsystemTestCase {

   private Map<String,Object> camera = CamerasFixtures.buildCamera().create();
   private String cameraAddr;

   private void addAll() {
      cameraAddr = addModel(camera).getAddress().getRepresentation();
   }

   @Test
   public void testSyncOnLoadNoCameras() {
      start();
      assertFalse(context.model().getAvailable());
      assertNoCameras();
   }

   @Test
   public void testSyncOnLoadWithCameras() {
      addAll();
      start();
      assertTrue(context.model().getAvailable());
      assertCameras(cameraAddr);
   }

   @Test
   public void testRemoveCamera() {
      addAll();
      start();
      removeModel(cameraAddr);
      assertFalse(context.model().getAvailable());
   }

   @Test
   public void testCameraConnectivityChange() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      updateModel(cameraAddr, update);
      assertOffline(cameraAddr);

      update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      updateModel(cameraAddr, update);
      assertOfflineEmpty();

      update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      updateModel(cameraAddr, update);
      assertOffline(cameraAddr);
   }
}

