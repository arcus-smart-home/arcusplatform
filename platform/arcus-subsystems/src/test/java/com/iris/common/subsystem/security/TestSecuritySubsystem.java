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
package com.iris.common.subsystem.security;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.SecurityAlarmModeCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmResponse;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.SimpleModelStore;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.CallTreeEntry;
import com.iris.util.IrisCollections;
import com.iris.util.TypeMarker;

public class TestSecuritySubsystem extends SecuritySubsystemTestCase {
   private static final Logger LOGGER = LoggerFactory.getLogger(TestSecuritySubsystem.class); 
   
   
   @Test
   public void testOnConnectivityStateChange() {
      String address = glassbreakSensor.getAddress().getRepresentation();
      
      startSubsystem();
      
      takeDeviceOffline(glassbreakSensor);
      assertTrue("glassbreak sensor should be offline",model.getOfflineDevices().contains(address));
      assertFalse(model.getReadyDevices().contains(address));
      assertFalse(model.getTriggeredDevices().contains(address));
      
      takeDeviceOnline(glassbreakSensor);
      assertFalse("glassbreak sensor should be offline",model.getOfflineDevices().contains(address));
      assertFalse("glassbreak sensor should not be triggered",model.getTriggeredDevices().contains(glassbreakSensor.getAddress().getRepresentation()));
      assertTrue("glassbreak sensor should be ready",model.getReadyDevices().contains(glassbreakSensor.getAddress().getRepresentation()));
   
   }
   
   
   @Test
   public void testOnAdd() {
      subsystem.onAdded(context);
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_DISARMED, model.getAlarmState());
      assertEquals(SecuritySubsystemCapability.ALARMMODE_OFF, model.getAlarmMode());
      assertTrue(model.getSecurityDevices().size() == 0);
      assertTrue(model.getReadyDevices().size() == 0);
      assertTrue(model.getArmedDevices().size() == 0);
      assertTrue(model.getOfflineDevices().size() == 0);
      assertTrue(model.getBypassedDevices().size() == 0);
      assertTrue(model.getTriggeredDevices().size() == 0);
      assertTrue(model.getCallTree().size() == 0);
      assertTrue(SecuritySubsystemUtil.getPartialDevices(context).size() == 0);
      assertTrue(SecuritySubsystemUtil.getOnDevices(context).size() == 0);
      assertEquals(0, context.model().getAttribute(SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON));
      assertEquals(0, context.model().getAttribute(SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_PARTIAL));
   }

   
   @Test
   public void testOnStarted() {
      startSubsystem();
      SecuritySubsystemModel model = context.model();
      Set<String> securityDevices = context.model().getSecurityDevices();
      Set<String> partialDevices = model.getAttribute(TypeMarker.setOf(String.class), SecuritySubsystemUtil.DEVICES_KEY_PARTIAL).get();
      Set<String> onDevices = context.model().getAttribute(TypeMarker.setOf(String.class), SecuritySubsystemUtil.DEVICES_KEY_ON).get();

      assertFalse(securityDevices.contains(nonSecurityDevice.getAddress().getRepresentation()));
      assertTrue(securityDevices.contains(glassbreakSensor.getAttribute(Capability.ATTR_ADDRESS)));
      assertTrue(securityDevices.contains(motionSensor.getAttribute(Capability.ATTR_ADDRESS)));
      assertTrue(securityDevices.contains(contactSensor.getAddress().getRepresentation()));

      assertTrue(onDevices.contains(glassbreakSensor.getAttribute(Capability.ATTR_ADDRESS)));
      assertTrue(onDevices.contains(motionSensor.getAddress().getRepresentation()));
      assertTrue(onDevices.contains(contactSensor.getAddress().getRepresentation()));

      // Partial Devices
      assertTrue(partialDevices.contains(contactSensor.getAddress().getRepresentation()));
      assertTrue(partialDevices.contains(glassbreakSensor.getAttribute(Capability.ATTR_ADDRESS)));

      // Call Tree
      assertTrue(context.model().getCallTreeEnabled());
      assertEquals(1, context.model().getCallTree().size());
      assertEquals(
            "SERV:person:" + accountModel.getAttribute(AccountCapability.ATTR_OWNER),
            context.model().getCallTree().get(0).get(CallTreeEntry.ATTR_PERSON)
      );

      //Offline Devices
      assertTrue(model.getOfflineDevices().contains(offlineSecurityDevice.getAddress().getRepresentation()));
      assertFalse("offline devices should be in ready",model.getReadyDevices().contains(offlineSecurityDevice.getAddress().getRepresentation()));
      
      //Keypads
//      assertTrue("keypad should be populated",model.getKeypads().contains(keyPad.getAddress().getRepresentation()));
//      assertFalse("keypads with motion should be ignored",model.getSecurityDevices().contains(keyPad.getAddress().getRepresentation()));
      
      assertEquals(numOfMotionSensors, model.getAttribute(SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON));
      assertEquals(0, model.getAttribute(SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_PARTIAL));
   }
   
   @Test
   public void testOnArmTwiceBypassed() {
      startSubsystem();
      faultDevice(contactSensor);
      armOnKeypad();
      subsystem.onArmingUnavailableResponse(securityPlatformMessage(MessageBody.buildMessage("test", new HashMap<String, Object>())), context);
      armOnKeypad();
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMING,model.getAlarmState());
      timeout();
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMED,model.getAlarmState());
   
   }

   @Test
   public void testOnArmAllReadyPartial() {
      // record this at the start of the test case for slow build servers
      long ts = System.currentTimeMillis() + 10000;
      
      startSubsystem();
      model.setAttribute(SecurityAlarmModeCapability.ATTR_EXITDELAYSEC + ":" + SecuritySubsystemCapability.ALARMMODE_PARTIAL, new Integer(10));
      MessageBody armResponse = armPartial();
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMING, context.model().getAlarmState());
      assertEquals(new Integer(10), ArmResponse.getDelaySec(armResponse));
      Date timeout = SubsystemUtils.getTimeout(context).get();
      assertTrue("Expected timeout of about " + new Date(ts) + " but was " + timeout, ts <= timeout.getTime());

      timeout();
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMED, context.model().getAlarmState());
      assertTrue("Should contain contact and glassbreak", context.model().getArmedDevices().containsAll(IrisCollections.setOf(glassbreakSensor.getAddress().getRepresentation(), contactSensor.getAddress().getRepresentation())));
      assertFalse("Should not contain motion", context.model().getArmedDevices().containsAll(IrisCollections.setOf(motionSensor.getAddress())));
      assertFalse("Should not be in ready state", context.model().getReadyDevices().contains(glassbreakSensor.getAttribute(Capability.ATTR_ADDRESS)));

      disarmSystem();
      assertFalse("Should not contain contact and glassbreak", context.model().getArmedDevices().containsAll(IrisCollections.setOf(glassbreakSensor.getAddress().getRepresentation(), contactSensor.getAddress().getRepresentation())));
      assertTrue("Should contain contact and glassbreak", context.model().getReadyDevices().containsAll(IrisCollections.setOf(glassbreakSensor.getAddress().getRepresentation(), contactSensor.getAddress().getRepresentation())));

   }
   @Test
   public void testSyncDevices() {
      startSubsystem();
      contactSensor.setAttribute(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED);
      initModelStore();
      subsystem.syncDeviceState(context);
      assertFalse("Should not contain contact sensor", context.model().getReadyDevices().contains(contactSensor.getAddress().getRepresentation()));
   }   

   @Test
   public void onPersonAdded() {
      startSubsystem();
      ModelAddedEvent event = new ModelAddedEvent(owner.getAddress());
      subsystem.onPersonAdded(event, context);
      assertTrue("person should be added to the tree", addressExists((String) owner.getAddress().getRepresentation(), model.getCallTree()));

      ModelRemovedEvent revent = ModelRemovedEvent.create(new SimpleModel(owner));
      subsystem.onPersonRemoved(revent, context);
      assertFalse("person should be removed from the tree", addressExists((String) owner.getAddress().getRepresentation(), model.getCallTree()));

   }

   @Test
   public void shouldPopulateAlertTriggersList() {
      startSubsystem();
      faultDevice(motionSensor);
      assertFalse("should not contain an alert - disarmed", model.getLastAlertTriggers().containsKey(motionSensor.getAddress().getRepresentation()));
      clearDevice(motionSensor);
      armPartial();
      faultDevice(motionSensor);
      assertFalse("should not contain an alert - arming", model.getLastAlertTriggers().containsKey(motionSensor.getAddress().getRepresentation()));
      clearDevice(motionSensor);
      disarmSystem();
      immediateArmOn();
      faultDevice(motionSensor);
      assertTrue("should contain an alert", model.getLastAlertTriggers().containsKey(motionSensor.getAddress().getRepresentation()));
      timeout();
      faultDevice(contactSensor);
      
      assertTrue("should contain a contact sensor trigger", model.getLastAlertTriggers().containsKey(contactSensor.getAddress().getRepresentation()));
      disarmSystem();
      assertTrue("should clear alert triggers", model.getLastAlertTriggers().isEmpty());
   }

   @Test
   public void testAddRemovePerson() {
      startSubsystem();
      ModelAddedEvent event = new ModelAddedEvent(owner.getAddress());
      subsystem.onPersonAdded(event, context);
      assertTrue("person should be added to the tree", addressExists(owner.getAddress().getRepresentation(), model.getCallTree()));

      ModelRemovedEvent revent = ModelRemovedEvent.create(new SimpleModel(owner));
      subsystem.onPersonRemoved(revent, context);
      assertFalse("person should be removed from the tree", addressExists(owner.getAddress().getRepresentation(), model.getCallTree()));

   }

   @Test
   public void testSubsystemAvailable() {
      startSubsystem();
      assertTrue(model.getAvailable());
      removeDevice(contactSensor);
      removeDevice(motionSensor);
      removeDevice(glassbreakSensor);
      removeDevice(offlineSecurityDevice);
   }
   
   @Test
   public void testMakeDevicePartial(){
      startSubsystem();
      
      Set<String> partialDevices = new HashSet<>(SecuritySubsystemUtil.getPartialDevices(context));
      assertFalse(partialDevices.contains(motionSensor.getAddress().getRepresentation()));
      
      partialDevices.add(motionSensor.getAddress().getRepresentation());
      setAttributes(ImmutableMap.<String, Object>of(SecuritySubsystemUtil.DEVICES_KEY_PARTIAL, partialDevices));

      assertTrue(SecuritySubsystemUtil.getPartialDevices(context).contains(motionSensor.getAddress().getRepresentation()));
      assertEquals(numOfMotionSensors, (int)context.model().getAttribute(SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON));
      assertEquals(1, (int)context.model().getAttribute(SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_PARTIAL));
   }
   
   @Test
   public void testArmWithTriggeredDevices() {
      startSubsystem();
      faultDevice(contactSensor);
      boolean thrown = false;
      try {
         armPartial();
      } catch (ErrorEventException eee) {
         thrown = true;
      }
      assertTrue("should have thrown exception", thrown);
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_DISARMED, context.model().getAlarmState());
   }
   
   @Test
   public void testArmWithTriggeredMotionDevices() {
      startSubsystem();
      faultDevice(motionSensor);
      armOn();     
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMING, context.model().getAlarmState());
   }
   
   @Test
   public void testArmBypassedWithTriggeredMotionAndContactDevice() {
	   doTestArmBypassedWithTriggeredMotionAndContactDevice(SecuritySubsystemV1.MODE_ON);
   }
   
   @Test
   public void testArmBypassedWithTriggeredMotionAndContactDevice2() {
	   doTestArmBypassedWithTriggeredMotionAndContactDevice(SecuritySubsystemV1.MODE_PARTIAL);
   }
   
   private void doTestArmBypassedWithTriggeredMotionAndContactDevice(String armMode) {
		  long ts = System.currentTimeMillis() + 10000;
	      startSubsystem();
	      faultDevice(motionSensor);
	      faultDevice(contactSensor);
	      boolean thrown = false;
	      try {
	    	  armPartial();
		  } catch (ErrorEventException eee) {
		      thrown = true;
		  }
	      assertTrue("should have thrown exception", thrown);
	      
	      armBypassed(armMode);
	      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMING, context.model().getAlarmState());
	      
	      Date timeout = SubsystemUtils.getTimeout(context).get();
	      assertTrue("Expected timeout of about " + new Date(ts) + " but was " + timeout, ts <= timeout.getTime());
	      
	      timeout();
	      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMED, context.model().getAlarmState());
	      assertTrue("should have contact sensor bypassed", context.model().getBypassedDevices().contains(contactSensor.getAddress().getRepresentation()));
	      assertFalse("should NOT have motion sensor bypassed", context.model().getBypassedDevices().contains(motionSensor.getAddress().getRepresentation()));

   }
   
   @Test
   public void testArmOnKeypadWithTriggeredMotionDevices() {
      startSubsystem();
      faultDevice(motionSensor);
      armOnKeypad();    
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMING, context.model().getAlarmState());
   }
   
   @Test
   public void testArmWithTriggeredDevicesNotInCurrentMode() {
      startSubsystem();
      faultDevice(motionSensor);
      armPartial();
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMING, context.model().getAlarmState());
   }

   @Test
   public void testArmBypassed() {
      // record this at the start of the test case for slow build servers
      long ts = System.currentTimeMillis() + 10000;
      
      startSubsystem();
      faultDevice(contactSensor);
      armBypassed();
      Date timeout = SubsystemUtils.getTimeout(context).get();
      assertTrue("Expected timeout of about " + new Date(ts) + " but was " + timeout, ts <= timeout.getTime());
      
      timeout();
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMED, context.model().getAlarmState());
      assertTrue("should have contact sensor bypassed", context.model().getBypassedDevices().contains(contactSensor.getAddress().getRepresentation()));

   }

   @Test
   public void testContactSensorTriggerAndClear() {
      startSubsystem();
      faultDevice(contactSensor);
      assertTrue(model.getTriggeredDevices().contains(contactSensor.getAddress().getRepresentation()));
      clearDevice(contactSensor);
      assertFalse(model.getTriggeredDevices().contains(contactSensor.getAddress().getRepresentation()));
   }
   
   
   @Test
   public void testMotorizedDoorTriggerAndClear() {
      startSubsystem();
      faultDevice(noneGenieMotorizedDoor);
      //TODO - BACK OUT ITWO-6269 
      //assertTrue(model.getTriggeredDevices().contains(noneGenieMotorizedDoor.getAddress().getRepresentation()));
      assertFalse(model.getTriggeredDevices().contains(noneGenieMotorizedDoor.getAddress().getRepresentation()));
      clearDevice(noneGenieMotorizedDoor);
      assertFalse(model.getTriggeredDevices().contains(noneGenieMotorizedDoor.getAddress().getRepresentation()));
   }
   
   @Test
   public void testBlacklistedMotorizedDoorTriggerAndClearAndRemove() {
	  String productId = "aeda44";
	  
      startSubsystem();
      Map<String, Object> genieMotorizedDoorAttribs = ModelFixtures.createMotorizedDoorFixture();
      genieMotorizedDoorAttribs.put(DeviceCapability.ATTR_PRODUCTID, productId);
      Model genieMotorizedDoor = addModel(genieMotorizedDoorAttribs);
      //TODO - BACK OUT ITWO-6269 
      //assertTrue(model.getBlacklistedSecurityDevices().contains(genieMotorizedDoor.getAddress().getRepresentation()));
      assertFalse(model.getBlacklistedSecurityDevices().contains(genieMotorizedDoor.getAddress().getRepresentation()));
      assertFalse(model.getSecurityDevices().contains(genieMotorizedDoor.getAddress().getRepresentation()));
      
      faultDevice(genieMotorizedDoor);
      assertFalse(model.getTriggeredDevices().contains(noneGenieMotorizedDoor.getAddress().getRepresentation()));
      clearDevice(genieMotorizedDoor);
      assertFalse(model.getTriggeredDevices().contains(noneGenieMotorizedDoor.getAddress().getRepresentation()));
      
      removeModel(genieMotorizedDoor);
      assertFalse(model.getBlacklistedSecurityDevices().contains(genieMotorizedDoor.getAddress().getRepresentation()));

   }

   @Test
   public void testMotionSensorTriggerAndClear() {
      startSubsystem();
      faultDevice(motionSensor);
      assertTrue(model.getTriggeredDevices().contains(motionSensor.getAddress().getRepresentation()));
      clearDevice(motionSensor);
      assertFalse(model.getTriggeredDevices().contains(motionSensor.getAddress().getRepresentation()));
   }

   @Test
   public void testContactSensorTriggerAndClearDevice() {
      startSubsystem();
      assertTrue(model.getReadyDevices().contains(glassbreakSensor.getAttribute(GlassCapability.ATTR_ADDRESS)));
      ModelChangedEvent breakEvent = ModelChangedEvent.create(Address.fromString((String) glassbreakSensor.getAttribute(GlassCapability.ATTR_ADDRESS)), GlassCapability.ATTR_BREAK, GlassCapability.BREAK_DETECTED, null);
      subsystem.onGlassBreak(breakEvent, context);
      assertTrue(model.getTriggeredDevices().contains(glassbreakSensor.getAttribute(GlassCapability.ATTR_ADDRESS)));
      assertFalse(model.getReadyDevices().contains(glassbreakSensor.getAttribute(GlassCapability.ATTR_ADDRESS)));

      ModelChangedEvent clearEvent = ModelChangedEvent.create(Address.fromString((String) glassbreakSensor.getAttribute(GlassCapability.ATTR_ADDRESS)), GlassCapability.ATTR_BREAK, GlassCapability.BREAK_SAFE, null);
      subsystem.onGlassBreak(clearEvent, context);
      assertFalse(model.getTriggeredDevices().contains(glassbreakSensor.getAttribute(GlassCapability.ATTR_ADDRESS)));
   }

   @Test
   public void testOnAddMotionDevice() {
      startSubsystem();
      Map<String, Object> motionSensor2 = com.iris.common.subsystem.security.SecurityFixtures.createMotionFixture();
      ((SimpleModelStore) context.models()).addModel(motionSensor2);

      ModelAddedEvent event = new ModelAddedEvent(Address.fromString((String) motionSensor2.get(Capability.ATTR_ADDRESS)));
      subsystem.onDeviceAdded(event, context);

      Set<String> securityDevices = context.model().getSecurityDevices();
      Set<String> partialDevices = SecuritySubsystemUtil.getPartialDevices(context);
      Set<String> onDevices = SecuritySubsystemUtil.getOnDevices(context);

      assertTrue(securityDevices.contains(motionSensor2.get(Capability.ATTR_ADDRESS)));
      assertTrue(onDevices.contains(motionSensor2.get(Capability.ATTR_ADDRESS)));
      assertFalse(partialDevices.contains(motionSensor2.get(Capability.ATTR_ADDRESS)));
      
      assertEquals(numOfMotionSensors+1, context.model().getAttribute(SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_ON));
      assertEquals(0, context.model().getAttribute(SecuritySubsystemUtil.MOTIONSENSOR_COUNT_KEY_PARTIAL));
   }

   @Test
   public void testOnGlassBreakDisarmed() {
      startSubsystem();
      context.model().setAlarmState(SecuritySubsystemCapability.ALARMSTATE_DISARMED);
      ModelChangedEvent event = ModelChangedEvent.create(Address.platformDriverAddress(UUID.randomUUID()), GlassCapability.ATTR_BREAK, GlassCapability.BREAK_DETECTED, null);
      subsystem.onGlassBreak(event, context);
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_DISARMED, model.getAlarmState());
   }

   @Test
   public void testOnGlassBreakTriggeredArmed() {
      startSubsystem();
      armPartial();
      timeout(); // advance through arming
      faultDevice(glassbreakSensor);
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_SOAKING, model.getAlarmState());
      assertTrue(model.getTriggeredDevices().contains(glassbreakSensor.getAddress().getRepresentation()));
      timeout(); // advance through soaking
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ALERT, model.getAlarmState());
      assertEquals("ALARM", model.getLastAlertCause());

   }
   
   @Test
   public void testPanic() {
      startSubsystem();
      subsystem.onPanicRequest(securityPlatformMessage(MessageBody.buildMessage("test", new HashMap<String, Object>())), context);
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ALERT, model.getAlarmState());
      assertEquals("panic", model.getLastAlertCause());
   }   
   
   @Test
   public void testPanicTwice() {
      startSubsystem();
      subsystem.onPanicRequest(securityPlatformMessage(MessageBody.buildMessage("test", new HashMap<String, Object>())), context);
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ALERT, model.getAlarmState());
      Date panicTime = model.getLastAlertTime();
      subsystem.onPanicRequest(securityPlatformMessage(MessageBody.buildMessage("test", new HashMap<String, Object>())), context);
      assertEquals(panicTime, model.getLastAlertTime());
      assertEquals("panic", model.getLastAlertCause());
   }   

   @Test
   public void testSensitivityOnWithDeviceCleared() {
      context.model().setAttribute(SecurityAlarmModeCapability.ATTR_ALARMSENSITIVITYDEVICECOUNT+":"+SecuritySubsystemCapability.ALARMMODE_ON, 1);
      startSubsystem();
      immediateArmOn();
      faultDevice(glassbreakSensor);
      clearDevice(glassbreakSensor);
      timeout(); // advance through soaking
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ALERT, model.getAlarmState());
   }

   
   @Test
   public void testSensitivityOn() {
      context.model().setAttribute(SecurityAlarmModeCapability.ATTR_ALARMSENSITIVITYDEVICECOUNT+":"+SecuritySubsystemCapability.ALARMMODE_ON, 2);
      startSubsystem();
      immediateArmOn();
      faultDevice(glassbreakSensor);
      clearDevice(glassbreakSensor);
      timeout(); // advance through soaking
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMED, model.getAlarmState());
      faultDevice(motionSensor);
      timeout();
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMED, model.getAlarmState());
      faultDevice(motionSensor);
      faultDevice(glassbreakSensor);
      timeout();
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ALERT, model.getAlarmState());
   }
   
   @Test
   public void testSensitivityPartial() {
      context.model().setAttribute(SecurityAlarmModeCapability.ATTR_ALARMSENSITIVITYDEVICECOUNT+":"+SecuritySubsystemCapability.ALARMMODE_PARTIAL, 2);
      Model glass2 = new SimpleModel(com.iris.common.subsystem.security.SecurityFixtures.createGlassbreakFixture());
      ((SimpleModelStore) context.models()).addModel(glass2.toMap());

      startSubsystem();
      armPartial();
      timeout(); // advance through arming
      faultDevice(motionSensor); //not in partial
      faultDevice(glassbreakSensor); //trigger 1
      timeout(); // advance through soaking
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMED, model.getAlarmState());
      faultDevice(contactSensor); //trigger2
      faultDevice(motionSensor); //not in partial
      faultDevice(glassbreakSensor); //trigger 1
      timeout(); // advance through soaking
      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ALERT, model.getAlarmState());

   }   
   
   @Test
   public void testOnRemoveDevice() {
      startSubsystem();
      faultDevice(contactSensor);
      removeDevice(contactSensor);
      assertFalse(context.model().getSecurityDevices().contains(contactSensor.getAddress().getRepresentation()));
      assertFalse(SecuritySubsystemUtil.getPartialDevices(context).contains(contactSensor.getAddress().getRepresentation()));
      assertFalse(SecuritySubsystemUtil.getOnDevices(context).contains(contactSensor.getAddress().getRepresentation()));
      assertFalse(model.getTriggeredDevices().contains(contactSensor.getAddress().getRepresentation()));

      removeDevice(motionSensor);
      removeDevice(glassbreakSensor);
      removeDevice(offlineSecurityDevice);
      removeDevice(noneGenieMotorizedDoor);
      assertFalse(context.model().getAvailable());
   }
   
   
}

