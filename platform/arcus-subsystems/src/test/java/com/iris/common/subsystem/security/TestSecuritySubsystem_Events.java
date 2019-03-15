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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SecuritySubsystemCapability;

public class TestSecuritySubsystem_Events extends SecuritySubsystemTestCase {
   private static final Logger LOGGER = LoggerFactory.getLogger(TestSecuritySubsystem_Events.class); 
   
   
   @Test
   public void testBroadEventOnDisarmed_fromClient() {
	  
	   doArm(clientAddress);
	   
	   disarmSystem(clientAddress);
	   assertEquals(SecuritySubsystemCapability.ALARMSTATE_DISARMED, context.model().getAlarmState());	   
	   assertContainsBroadcastEventWithAttrs(SecuritySubsystemCapability.DisarmedEvent.NAME, 
			   ImmutableMap.<String,Object>of(
					   SecuritySubsystemCapability.DisarmedEvent.ATTR_METHOD,SecuritySubsystemCapability.ArmedEvent.METHOD_CLIENT,
					   SecuritySubsystemCapability.DisarmedEvent.ATTR_BY, owner.getAddress().getRepresentation()), 1);
   
   }
   
   @Test
   public void testBroadEventOnDisarmed_fromDevice() {
	  
	   doArm(keyPad.getAddress());
	   
	   disarmSystem(keyPad.getAddress());
	   assertEquals(SecuritySubsystemCapability.ALARMSTATE_DISARMED, context.model().getAlarmState());	   
	   assertContainsBroadcastEventWithAttrs(SecuritySubsystemCapability.DisarmedEvent.NAME, 
			   ImmutableMap.<String,Object>of(
					   SecuritySubsystemCapability.DisarmedEvent.ATTR_METHOD,SecuritySubsystemCapability.ArmedEvent.METHOD_DEVICE,
					   SecuritySubsystemCapability.DisarmedEvent.ATTR_BY, owner.getAddress().getRepresentation()), 1);
   
   }
   
   
   @Test
   public void testBroadEventOnArmed_fromClient() {
	   doArm(clientAddress);
   
	   assertContainsBroadcastEventWithAttrs(SecuritySubsystemCapability.ArmedEvent.NAME, 
			   ImmutableMap.<String,Object>of(
					   SecuritySubsystemCapability.ArmedEvent.ATTR_ALARMMODE,model.getAlarmMode(),
					   SecuritySubsystemCapability.ArmedEvent.ATTR_BYPASSEDDEVICES, NULL_VALUE,
					   SecuritySubsystemCapability.ArmedEvent.ATTR_METHOD,SecuritySubsystemCapability.ArmedEvent.METHOD_CLIENT,
					   SecuritySubsystemCapability.ArmedEvent.ATTR_BY, owner.getAddress().getRepresentation()), 1);
   }


	
   
   @Test
   public void testBroadEventOnArmed_fromDevice() {
	   
	   doArm(keyPad.getAddress());   
	   assertContainsBroadcastEventWithAttrs(SecuritySubsystemCapability.ArmedEvent.NAME, 
			   ImmutableMap.<String,Object>of(
					   SecuritySubsystemCapability.ArmedEvent.ATTR_ALARMMODE,model.getAlarmMode(),
					   SecuritySubsystemCapability.ArmedEvent.ATTR_BYPASSEDDEVICES, NULL_VALUE,
					   SecuritySubsystemCapability.ArmedEvent.ATTR_METHOD,SecuritySubsystemCapability.ArmedEvent.METHOD_DEVICE,
					   SecuritySubsystemCapability.ArmedEvent.ATTR_BY, owner.getAddress().getRepresentation()), 1);
   }
   
   @Test
   public void testBroadEventOnArmed_bypassed() {
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

      assertContainsBroadcastEventWithAttrs(SecuritySubsystemCapability.ArmedEvent.NAME, 
			   ImmutableMap.<String,Object>of(
					   SecuritySubsystemCapability.ArmedEvent.ATTR_ALARMMODE,model.getAlarmMode(),
					   SecuritySubsystemCapability.ArmedEvent.ATTR_BYPASSEDDEVICES, model.getBypassedDevices(),
					   SecuritySubsystemCapability.ArmedEvent.ATTR_METHOD,SecuritySubsystemCapability.ArmedEvent.METHOD_CLIENT,
					   SecuritySubsystemCapability.ArmedEvent.ATTR_BY, owner.getAddress().getRepresentation()), 1);
            
   }
   
   @Test
   public void testBroadEventOnAlert_Panic_fromClient() {
	   startSubsystem();
	   subsystem.onPanicRequest(securityPlatformMessage(MessageBody.buildMessage("test", new HashMap<String, Object>())), context);
	   
	   assertContainsBroadcastEventWithAttrs(SecuritySubsystemCapability.AlertEvent.NAME, 
			   ImmutableMap.<String,Object>of(
					   SecuritySubsystemCapability.AlertEvent.ATTR_CAUSE,model.getLastAlertCause(),
					   SecuritySubsystemCapability.AlertEvent.ATTR_TRIGGERS,model.getLastAlertTriggers(),
					   SecuritySubsystemCapability.AlertEvent.ATTR_METHOD,SecuritySubsystemCapability.AlertEvent.METHOD_PANIC,
					   SecuritySubsystemCapability.AlertEvent.ATTR_BY, clientAddress.getRepresentation()), 1);
   
   }
   
   @Test
   public void testBroadEventOnAlert_Panic_fromDevice() {
	   startSubsystem();
	   subsystem.onPanicRequest(securityPlatformMessage(MessageBody.buildMessage("test", new HashMap<String, Object>()), keyPad.getAddress()), context);
	   
	   assertContainsBroadcastEventWithAttrs(SecuritySubsystemCapability.AlertEvent.NAME, 
			   ImmutableMap.<String,Object>of(
					   SecuritySubsystemCapability.AlertEvent.ATTR_CAUSE,model.getLastAlertCause(),
					   SecuritySubsystemCapability.AlertEvent.ATTR_TRIGGERS,model.getLastAlertTriggers(),
					   SecuritySubsystemCapability.AlertEvent.ATTR_METHOD,SecuritySubsystemCapability.AlertEvent.METHOD_PANIC,
					   SecuritySubsystemCapability.AlertEvent.ATTR_BY, keyPad.getAddress().getRepresentation()), 1);
	   
	  // assertEquals(owner.getAddress(), context.geta););
   }
   
   
   @Test
   public void testBroadEventOnAlert_fromDevice() {
	   doArm(keyPad.getAddress());
	   faultDevice(contactSensor);
	   
	   timeout(); // advance through soaking
	   assertEquals(SecuritySubsystemCapability.ALARMSTATE_ALERT, model.getAlarmState());
	   assertEquals("ALARM", model.getLastAlertCause());
	      
	   assertContainsBroadcastEventWithAttrs(SecuritySubsystemCapability.AlertEvent.NAME, 
			   ImmutableMap.<String,Object>of(
					   SecuritySubsystemCapability.AlertEvent.ATTR_CAUSE,model.getLastAlertCause(),
					   SecuritySubsystemCapability.AlertEvent.ATTR_TRIGGERS,model.getLastAlertTriggers(),
					   SecuritySubsystemCapability.AlertEvent.ATTR_METHOD,SecuritySubsystemCapability.AlertEvent.METHOD_DEVICE,
					   SecuritySubsystemCapability.AlertEvent.ATTR_BY, keyPad.getAddress().getRepresentation()), 1);
   }
   
   @Test
   public void testBroadcastEventOnAlert_fromClientSoaking() {
	   startSubsystem();
	      armPartial();
	      timeout(); // advance through arming
	      faultDevice(glassbreakSensor);
	      assertEquals(SecuritySubsystemCapability.ALARMSTATE_SOAKING, model.getAlarmState());
	      assertTrue(model.getTriggeredDevices().contains(glassbreakSensor.getAddress().getRepresentation()));
	      timeout(); // advance through soaking
	      assertEquals(SecuritySubsystemCapability.ALARMSTATE_ALERT, model.getAlarmState());
	      assertEquals("ALARM", model.getLastAlertCause());
	      
	      assertContainsBroadcastEventWithAttrs(SecuritySubsystemCapability.AlertEvent.NAME, 
				   ImmutableMap.<String,Object>of(
						   SecuritySubsystemCapability.AlertEvent.ATTR_CAUSE,model.getLastAlertCause(),
						   SecuritySubsystemCapability.AlertEvent.ATTR_TRIGGERS,model.getLastAlertTriggers(),
						   SecuritySubsystemCapability.AlertEvent.ATTR_METHOD,SecuritySubsystemCapability.AlertEvent.METHOD_DEVICE,
						   SecuritySubsystemCapability.AlertEvent.ATTR_BY, clientAddress.getRepresentation()), 1);
   }
   
   
   
   private void doArm(Address fromAddress) {
		startSubsystem();	  
	   armOnKeypad(fromAddress);
	   assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMING,model.getAlarmState());
	   timeout();	      
	   assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMED,model.getAlarmState());
	}
}

