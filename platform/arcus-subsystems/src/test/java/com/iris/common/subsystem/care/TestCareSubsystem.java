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

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorAlertAcknowledgedEvent;
import com.iris.messages.model.Model;

public class TestCareSubsystem extends CareSubsystemTestCase{

   ////////////////////////////////////////////////////////////////////
   // Authorization Test
   @Test 
   public void testAvailable() {
      placeModel.setAttribute(PlaceCapability.ATTR_SERVICELEVEL, "BASIC");
      assertFalse(context.model().getAvailable());

      placeModel.setAttribute(PlaceCapability.ATTR_SERVICELEVEL, "PREMIUM");
      addModel(CareSubsystemTestFixtures.createMotionFixture());
      assertTrue(context.model().getAvailable());   
   }
   
   ///////////////////////////////////////////////////////////////////
   // Care was rolled into prod without care devices being populated.
   // So lets sync those careDevices with careCapableDevices if its empty
   @Test public void testFixCareDevices() {      
      Model motion = addModel(CareSubsystemTestFixtures.createMotionFixture());
      model.setCareDevices(null);
      model.setCareDevicesPopulated(null);
      careSS.onStarted(context);
      assertCareDevices(motion.getAddress().getRepresentation());
      assertTrue(model.getCareDevicesPopulated());
   }

   
   ////////////////////////////////////////////////////////////////////
   // Basic device add/remove tests
   @Test public void testAddNonCareDevice() {      
      addModel(CareSubsystemTestFixtures.createSwitchFixture());
      addModel(CareSubsystemTestFixtures.createKeypadV2Fixture());
      assertFalse(context.model().getAvailable());
   }

   @Test public void testAddCareDevice() {
      addModel(CareSubsystemTestFixtures.createMotionFixture());
      assertTrue(context.model().getAvailable());   
   }
   
   @Test
   public void testAddPresenceDevice() {
   	assertFalse(context.model().getAvailable());
   	Model fob1 = addModel(CareSubsystemTestFixtures.createPresenceFixture());
   	assertTrue(context.model().getAvailable()); 
   	assertEquals(1, context.model().getPresenceDevices().size());
   	
   	removeModel(fob1.getAddress());
   	assertFalse(context.model().getAvailable());
   	assertEquals(0, context.model().getPresenceDevices().size());
   }
   
   @Test public void testRemoveNonCareDevice() {      
      Model motion = addModel(CareSubsystemTestFixtures.createMotionFixture());
      Model swtch = addModel(CareSubsystemTestFixtures.createSwitchFixture());
      assertTrue(context.model().getAvailable());   
      removeModel(swtch);
      assertTrue(context.model().getAvailable());
   }


   @Test public void testRemoveCareDevice() {      
      Model motion = addModel(CareSubsystemTestFixtures.createMotionFixture());
      Model swtch = addModel(CareSubsystemTestFixtures.createSwitchFixture());
      assertTrue(context.model().getAvailable());   
      removeModel(motion);
      assertFalse(context.model().getAvailable());
   }

   //wds - added for https://eyeris.atlassian.net/browse/ITWO-11167
   @Test public void testPresenceNoLongerCareCapableDevice() {
      Model presence = addModel(CareSubsystemTestFixtures.createPresenceFixture());
      assertNoCareCapableDevices();
      assertNoCareDevices();

      addModel(CareSubsystemTestFixtures.createMotionFixture());
      assertTrue(context.model().getAvailable());
   }

   ////////////////////////////////////////////////////////////////////
   // Motion Sensor Tests
   @Test
   public void testAddMotionSensor() {  
      Model motion = addModel(CareSubsystemTestFixtures.createMotionFixture());
      this.assertCareCabableDevices(motion.getAddress().getRepresentation());
      this.assertCareDevices(motion.getAddress().getRepresentation());
      assertTrue(context.model().getAvailable());   
   }
   @Test
   public void testRemoveMotionSensor() { 
      Model motion = addModel(CareSubsystemTestFixtures.createMotionFixture());
      assertTrue(context.model().getAvailable());   
      removeModel(motion);
      assertNoCareCapableDevices();
      assertNoCareDevices();
      assertFalse(context.model().getAvailable());
   }   
   @Test
   public void testActivateMotionSensor() {
      Model motion = addModel(CareSubsystemTestFixtures.createMotionFixture());
      Map<String,Object> update = ImmutableMap.<String,Object>of(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED);
      updateModel(motion.getAddress().getRepresentation(),update);
      this.assertActiveDevices(motion.getAddress().getRepresentation());
   }   
   @Test
   public void testInactivateMotionSensor() {      
      Model motion = addModel(CareSubsystemTestFixtures.createMotionFixture());
      Map<String,Object> update = ImmutableMap.<String,Object>of(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE);
      updateModel(motion.getAddress().getRepresentation(),update);
      this.assertInactiveDevices(motion.getAddress().getRepresentation());
   }
   
   ////////////////////////////////////////////////////////////////////
   // Contact Sensor Tests
   @Test
   public void testAddContactSensor() {
      Model contact = addModel(CareSubsystemTestFixtures.createContactFixture());
      this.assertCareCabableDevices(contact.getAddress().getRepresentation());
      assertTrue(context.model().getAvailable());   
   }   
   @Test
   public void testRemoveContactSensor() {
      Model contact = addModel(CareSubsystemTestFixtures.createContactFixture());
      assertTrue(context.model().getAvailable());   
      removeModel(contact);
      assertNoCareCapableDevices();
      assertFalse(context.model().getAvailable());
   }
   @Test
   public void testInactivateContactSensor() {      
      Model contact = addModel(CareSubsystemTestFixtures.createContactFixture());
      Map<String,Object> update = ImmutableMap.<String,Object>of(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED);
      updateModel(contact.getAddress().getRepresentation(),update);
      this.assertActiveDevices(contact.getAddress().getRepresentation());
   }
   
   
   @Test
   public void testOfflineContactSensor() {      
      Model contact = addModel(CareSubsystemTestFixtures.createContactFixture());
      Map<String,Object> update = ImmutableMap.<String,Object>of(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED);
      updateModel(contact.getAddress().getRepresentation(),update);
      this.assertActiveDevices(contact.getAddress().getRepresentation());
      
      update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      updateModel(contact.getAddress().getRepresentation(),update);
      this.assertInactiveDevices(contact.getAddress().getRepresentation());
      this.assertCareCabableDevices(contact.getAddress().getRepresentation());
      assertFalse( context.model().getTriggeredDevices().contains(contact.getAddress().getRepresentation()) );
   }
   
   
   
   @Test
   public void testAddOfflineContactSensor() {      
	   Map<String, Object> attrs = CareSubsystemTestFixtures.createContactFixture();
	   attrs.put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
	   attrs.put(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED);
       Model contact = addModel(attrs);
       this.assertInactiveDevices(contact.getAddress().getRepresentation());
       this.assertCareCabableDevices(contact.getAddress().getRepresentation());
       assertFalse( context.model().getTriggeredDevices().contains(contact.getAddress().getRepresentation()) );
      
      ImmutableMap<String, Object> update = ImmutableMap.<String,Object>of(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      updateModel(contact.getAddress().getRepresentation(),update);
      this.assertActiveDevices(contact.getAddress().getRepresentation());
      this.assertCareCabableDevices(contact.getAddress().getRepresentation());
      assertFalse( context.model().getInactiveDevices().contains(contact.getAddress().getRepresentation()) );
      
   }
   
   
   @Test
   public void testActivateContactSensor() {      
      Model contact = addModel(CareSubsystemTestFixtures.createContactFixture());
      Map<String,Object> update = ImmutableMap.<String,Object>of(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED);
      updateModel(contact.getAddress().getRepresentation(),update);
      this.assertInactiveDevices(contact.getAddress().getRepresentation());
   }   
   
   ////////////////////////////////////////////////////////////////////
   // Alarm Tests
   @Test
   public void testPanic() {  
      MessageBody panic = CareSubsystemCapability.PanicRequest.instance();
      PlatformMessage message = carePlatformMessage(panic);
      careSS.onPanic(message, context);
      assertLastCauseSet();
   }
   
   @Test
   public void testAcknowledge() {      
      testPanic();   // Get into panic mode.

      MessageBody ack = CareSubsystemCapability.AcknowledgeRequest.instance();
      PlatformMessage message = carePlatformMessage(ack);
      careSS.onAcknowledge(message, context);
      assertLastAckSet();
      
      MessageBody event = BehaviorAlertAcknowledgedEvent.instance();
      assertEquals(event.getAttributes(), broadcasts.getValue().getAttributes());
   }
   
   @Test
   public void testCleared() {
      testPanic();   // Get into panic mode.

      MessageBody clear = CareSubsystemCapability.ClearRequest.instance();
      PlatformMessage message = carePlatformMessage(clear);      
      careSS.onClear(message, context);
      assertLastClearSet();
   }
}

