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

import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.PetDoorCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.test.ModelFixtures;

public class TestDoorsNLocksSubsystem_Devices extends DoorsNLocksSubsystemTestCase {

   private Map<String,Object> lock = DoorsNLocksFixtures.buildLock().create();
   private Map<String,Object> door = DoorsNLocksFixtures.buildDoor().create();
   private Map<String,Object> sensor = DoorsNLocksFixtures.buildContact().create();
   private Map<String,Object> petdoor = ModelFixtures.buildDeviceAttributes(PetDoorCapability.NAMESPACE).create();
   private String lockAddr;
   private String doorAddr;
   private String sensorAddr;
   private String petdoorAddr;
   

   private void addAll() {
      lockAddr = addModel(lock).getAddress().getRepresentation();
      doorAddr = addModel(door).getAddress().getRepresentation();
      sensorAddr = addModel(sensor).getAddress().getRepresentation();
      petdoorAddr = addModel(petdoor).getAddress().getRepresentation();
   }

   @Test
   public void testSyncOnLoadNoDevices() {
      start();
      assertFalse(context.model().getAvailable());
      assertNoDoors();
      assertNoLocks();
      assertNoSensors();
   }

   @Test
   public void testSyncOnLoadWithDevices() {
      addAll();
      start();
      assertTrue(context.model().getAvailable());
      assertLockTotal(lockAddr);
      assertDoorsTotal(doorAddr);
      assertSensorTotal(sensorAddr);
   }

   @Test
   public void testRemoveLock() {
      addAll();
      start();
      removeModel(lockAddr);
      assertNoLocks();
      assertDoorsTotal(doorAddr);
      assertSensorTotal(sensorAddr);
   }

   @Test
   public void testRemoveDoor() {
      addAll();
      start();
      removeModel(doorAddr);
      assertLockTotal(lockAddr);
      assertNoDoors();
      assertSensorTotal(sensorAddr);
   }

   @Test
   public void testRemoveSensor() {
      addAll();
      start();
      removeModel(sensorAddr);
      assertLockTotal(lockAddr);
      assertDoorsTotal(doorAddr);
      assertNoSensors();
   }

   @Test
   public void testRemoveSensorUseHint() {
      Map<String,Object> update = ImmutableMap.<String,Object>of(ContactCapability.ATTR_USEHINT, ContactCapability.USEHINT_WINDOW);
      addAll();
      start();
      assertSensorTotal(sensorAddr);
      updateModel(sensorAddr, update);
      assertNoSensors();
   }

   @Test
   public void testAddSensorUpdateUseHint() {
      Map<String,Object> nonDoorSensor = DoorsNLocksFixtures.buildContact().put(ContactCapability.ATTR_USEHINT, ContactCapability.USEHINT_WINDOW).create();
      String sensorAddr = addModel(nonDoorSensor).getAddress().getRepresentation();
      start();
      assertNoSensors();
      Map<String,Object> update = ImmutableMap.<String, Object>of(ContactCapability.ATTR_USEHINT, ContactCapability.USEHINT_DOOR);
      updateModel(sensorAddr, update);
      assertSensorTotal(sensorAddr);
   }

   @Test
   public void testLockStateChange() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_UNLOCKED);
      updateModel(lockAddr, update);
      assertLockUnlocked(lockAddr);

      update = ImmutableMap.<String,Object>of(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKED);
      updateModel(lockAddr, update);
      assertLockUnlockedEmpty();
   }

   @Test
   public void testDoorStateChange() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN);
      updateModel(doorAddr, update);
      assertDoorOpened(doorAddr);

      update = ImmutableMap.<String, Object>of(MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED);
      updateModel(doorAddr, update);
      assertDoorsOpenEmpty();
   }

   @Test
   public void testSensorChange() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED);
      updateModel(sensorAddr, update);
      assertSensorOpened(sensorAddr);

      update = ImmutableMap.<String, Object>of(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED);
      updateModel(sensorAddr, update);
      assertSensorOpenEmpty();
   }

   @Test
   public void testLockConnectivityChange() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      updateModel(lockAddr, update);
      assertLockOffline(lockAddr);

      update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      updateModel(lockAddr, update);
      assertLockOfflineEmpty();
   }

   @Test
   public void testDoorConnectivityChange() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      updateModel(doorAddr, update);
      assertDoorOffline(doorAddr);

      update = ImmutableMap.<String, Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      updateModel(doorAddr, update);
      assertDoorsOfflineEmpty();
   }

   @Test
   public void testSensorConnectivityChange() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      updateModel(sensorAddr, update);
      assertSensorOffline(sensorAddr);

      update = ImmutableMap.<String, Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      updateModel(sensorAddr, update);
      assertSensorOfflineEmpty();
   }
   @Test
   public void testPetDoorAdd() {
      addAll();
      start();
      assertPetDoors(petdoorAddr);
   }
   
   @Test
   public void testPetDoorConnectivity() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      updateModel(petdoorAddr, update);
      assertOfflinePetDoors(petdoorAddr);
   }
   
   @Test
   public void testPetDoorAuto() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(PetDoorCapability.ATTR_LOCKSTATE, PetDoorCapability.LOCKSTATE_AUTO);
      updateModel(petdoorAddr, update);
      assertAutoPetDoors(petdoorAddr);
   }
   @Test
   public void testPetDoorUnlocked() {
      addAll();
      start();
      Map<String,Object> update = ImmutableMap.<String,Object>of(PetDoorCapability.ATTR_LOCKSTATE, PetDoorCapability.LOCKSTATE_UNLOCKED);
      updateModel(petdoorAddr, update);
      assertUnlockedPetDoors(petdoorAddr);
   }
   
}

