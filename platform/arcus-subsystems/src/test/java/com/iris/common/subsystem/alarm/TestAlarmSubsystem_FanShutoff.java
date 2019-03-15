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
package com.iris.common.subsystem.alarm;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CarbonMonoxideCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.capability.SpaceHeaterCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.model.test.ModelFixtures.DeviceBuilder;

public class TestAlarmSubsystem_FanShutoff extends PlatformAlarmSubsystemTestCase {
	private Model contact;
	private Model fan;
	private Model spaceheater;
	private Model thermostat;
   private Model smoke;
   private Model co;
   private Model fanWithSwitch;
	
	@Before
   public void createDevices() {
      // enable the alarms
      contact = addContactDevice();
      smoke = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
      co = addCODevice(true, CarbonMonoxideCapability.CO_SAFE);
      SecurityAlarmModeModel.setDevices(AlarmSubsystemCapability.SECURITYMODE_ON, securitySubsystem, ImmutableSet.of(contact.getAddress().getRepresentation()));
   }
	

	protected void start() throws Exception {
		init(subsystem);
		requests.reset();
	}
	
	@Test
   public void testSubystemStartNoCapableDevices() throws Exception {
      start();
      
      assertFalse(context.model().getFanShutoffSupported());
      assertTrue(context.model().getFanShutoffOnCO());
      assertFalse(context.model().getFanShutoffOnSmoke());     
   }
	
	@Test
   public void testSubystemAddOneCapableDevice() throws Exception {
      start();     
      //Test FanShutoffSupported flag based on capable devices add and remove
      fan = addFan(0);
      assertTrue(context.model().getFanShutoffSupported());      
      removeModel(fan);
      assertFalse(context.model().getFanShutoffSupported());
      fan = addFan(3);
      assertTrue(context.model().getFanShutoffSupported());      
      removeModel(fan);
      assertFalse(context.model().getFanShutoffSupported());
      
      fanWithSwitch = addFanWithSwitch(3, SwitchCapability.STATE_ON);
      assertTrue(context.model().getFanShutoffSupported());   
      removeModel(fanWithSwitch);
      assertFalse(context.model().getFanShutoffSupported());
      
      spaceheater = addSpaceHeater(SpaceHeaterCapability.HEATSTATE_OFF);
      assertTrue(context.model().getFanShutoffSupported());      
      removeModel(spaceheater);
      assertFalse(context.model().getFanShutoffSupported());
      spaceheater = addSpaceHeater(SpaceHeaterCapability.HEATSTATE_ON);
      assertTrue(context.model().getFanShutoffSupported());      
      removeModel(spaceheater);
      assertFalse(context.model().getFanShutoffSupported());
      
      thermostat = addThermostat(ThermostatCapability.HVACMODE_OFF);
      assertTrue(context.model().getFanShutoffSupported());      
      removeModel(thermostat);
      assertFalse(context.model().getFanShutoffSupported());
      thermostat = addThermostat(ThermostatCapability.HVACMODE_HEAT);
      assertTrue(context.model().getFanShutoffSupported());      
      removeModel(thermostat);
      assertFalse(context.model().getFanShutoffSupported());
      
      addModel(fan.toMap());
      addModel(spaceheater.toMap());
      addModel(thermostat.toMap());
      assertTrue(context.model().getFanShutoffSupported()); //3 capable devices, flag is true
      removeModel(fan.getAddress());
      assertTrue(context.model().getFanShutoffSupported()); //2 capable devices, flag is true
      removeModel(spaceheater.getAddress());
      assertTrue(context.model().getFanShutoffSupported()); //1 capable device, flag is true
      removeModel(thermostat.getAddress());
      assertFalse(context.model().getFanShutoffSupported());   //all capable devices removed, flag should be false.
   }
	
	


   @Test
	public void testTriggerCOAlarmWithFanShutoffOnCO_True() throws Exception {
	   expectAddAlert(CarbonMonoxideAlarm.NAME);
	   expectUpdateIncident();
      replay();      
      
      start();
      
      //Add all 3 capable devices that if triggered, should all be shut off
      fan = addFan(3);
      fanWithSwitch = addFanWithSwitch(3, SwitchCapability.STATE_ON);
      spaceheater = addSpaceHeater(SpaceHeaterCapability.HEATSTATE_ON);
      thermostat = addThermostat(ThermostatCapability.HVACMODE_HEAT);
      
      assertTrue(context.model().getFanShutoffSupported()); 
      assertTrue(context.model().getFanShutoffOnCO());
      
      arm(AlarmSubsystemCapability.SECURITYMODE_ON);
      
      trigger(co);
      
      assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(FanCapability.ATTR_SPEED, new Integer(0))); 
      assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(SpaceHeaterCapability.ATTR_HEATSTATE, SpaceHeaterCapability.HEATSTATE_OFF)); 
      assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF)); 
      assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF)); 
    
      verify();
	}
	
	@Test
   public void testTriggerCOAlarmWithFanShutoffOnCO_False() throws Exception {
      expectAddAlert(CarbonMonoxideAlarm.NAME);
      replay();      
      
      start();
      //Turn FANSHUTOFFONCO flag off
      model.setAttribute(AlarmSubsystemCapability.ATTR_FANSHUTOFFONCO, Boolean.FALSE); 
      
      //Add all 3 capable devices that if triggered, should all be shut off
      fan = addFan(3);
      spaceheater = addSpaceHeater(SpaceHeaterCapability.HEATSTATE_ON);
      thermostat = addThermostat(ThermostatCapability.HVACMODE_HEAT);
      
      assertTrue(context.model().getFanShutoffSupported()); 
      assertFalse(context.model().getFanShutoffOnCO());
      
      arm(AlarmSubsystemCapability.SECURITYMODE_ON);
      
      trigger(co);
      
      //Should not send shutoff messages
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(FanCapability.ATTR_SPEED, new Integer(0))); 
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(SpaceHeaterCapability.ATTR_HEATSTATE, SpaceHeaterCapability.HEATSTATE_OFF)); 
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF)); 
     
      verify();
   }
	
	
	@Test
   public void testTriggerCOAlarmWithFanShutoffOnCO_False_DeviceAlreadyOff() throws Exception {
      expectAddAlert(CarbonMonoxideAlarm.NAME);
      replay();      
      
      start();
      
      //Add all 3 capable devices that if triggered, should NOT be shut off because they are already off
      fan = addFan(0);
      fanWithSwitch = addFanWithSwitch(3, SwitchCapability.STATE_OFF);
      spaceheater = addSpaceHeater(SpaceHeaterCapability.HEATSTATE_OFF);
      thermostat = addThermostat(ThermostatCapability.HVACMODE_OFF);
      
      assertTrue(context.model().getFanShutoffSupported()); 
      assertTrue(context.model().getFanShutoffOnCO());
      
      arm(AlarmSubsystemCapability.SECURITYMODE_ON);
      
      trigger(co);
      
      //Should not send shutoff messages
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(FanCapability.ATTR_SPEED, new Integer(0))); 
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(SpaceHeaterCapability.ATTR_HEATSTATE, SpaceHeaterCapability.HEATSTATE_OFF)); 
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF)); 
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF)); 
     
      verify();
   }
	  
	
	@Test
   public void testTriggerSmokeAlarmWithFanShutoffOnSmoke_True() throws Exception {
      expectAddAlert(SmokeAlarm.NAME);
      expectUpdateIncident();
      replay();      
      
      start();
      //turn flag to be true
      model.setAttribute(AlarmSubsystemCapability.ATTR_FANSHUTOFFONSMOKE, Boolean.TRUE);       
      
      fan = addFan(3);
      spaceheater = addSpaceHeater(SpaceHeaterCapability.HEATSTATE_ON);
      thermostat = addThermostat(ThermostatCapability.HVACMODE_HEAT);
      
      assertTrue(context.model().getFanShutoffSupported()); 
      assertTrue(context.model().getFanShutoffOnSmoke());
      
      arm(AlarmSubsystemCapability.SECURITYMODE_ON);
      
      trigger(smoke);
      
      assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(FanCapability.ATTR_SPEED, new Integer(0))); 
      assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(SpaceHeaterCapability.ATTR_HEATSTATE, SpaceHeaterCapability.HEATSTATE_OFF)); 
      assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF)); 
     
      verify();
   }
	
   @Test
   public void testTriggerSmokeAlarmWithFanShutoffOnSmoke_False() throws Exception {
      expectAddAlert(SmokeAlarm.NAME);
      replay();      
      
      start();
      
      fan = addFan(3);
      spaceheater = addSpaceHeater(SpaceHeaterCapability.HEATSTATE_ON);
      thermostat = addThermostat(ThermostatCapability.HVACMODE_HEAT);
      
      assertTrue(context.model().getFanShutoffSupported()); 
      assertFalse(context.model().getFanShutoffOnSmoke());
      
      arm(AlarmSubsystemCapability.SECURITYMODE_ON);
      
      trigger(smoke);
      
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(FanCapability.ATTR_SPEED, new Integer(0))); 
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(SpaceHeaterCapability.ATTR_HEATSTATE, SpaceHeaterCapability.HEATSTATE_OFF)); 
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF)); 
     
      verify();
   }	
   
   @Test
   public void testTriggerSmokeAlarmWithFanShutoffOnSmoke_False_DeviceAlreadyOff() throws Exception {
      expectAddAlert(SmokeAlarm.NAME);
      replay();      
      
      start();
      
      //turn flag to be true
      model.setAttribute(AlarmSubsystemCapability.ATTR_FANSHUTOFFONSMOKE, Boolean.TRUE); 
      
      fan = addFan(0);
      spaceheater = addSpaceHeater(SpaceHeaterCapability.HEATSTATE_OFF);
      thermostat = addThermostat(ThermostatCapability.HVACMODE_OFF);
      
      assertTrue(context.model().getFanShutoffSupported()); 
      assertTrue(context.model().getFanShutoffOnSmoke());
      
      arm(AlarmSubsystemCapability.SECURITYMODE_ON);
      
      trigger(smoke);
      
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(FanCapability.ATTR_SPEED, new Integer(0))); 
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(SpaceHeaterCapability.ATTR_HEATSTATE, SpaceHeaterCapability.HEATSTATE_OFF)); 
      assertNotContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF)); 
     
      verify();
   }  
   
	protected Model addOnlineDevice(Map<String, Object> attribs, String... deviceNameSpace) {
       
	   DeviceBuilder m = ModelFixtures.buildDeviceAttributes(deviceNameSpace);
	   if(attribs != null && !attribs.isEmpty()) {
	      m.putAll(attribs);
	   }
	   m.put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
	   return addModel(m.create());
           
   }
	
	protected Model addFan(int fanSpeed) {
	   return addOnlineDevice(ImmutableMap.<String, Object>of(
         FanCapability.ATTR_SPEED, new Integer(fanSpeed)), FanCapability.NAMESPACE);
	}
	
	private Model addFanWithSwitch(int fanSpeed, String switchState)
   {
	   return addOnlineDevice(ImmutableMap.<String, Object>of(
         FanCapability.ATTR_SPEED, new Integer(fanSpeed), SwitchCapability.ATTR_STATE, switchState), FanCapability.NAMESPACE, SwitchCapability.NAMESPACE);
   }
	
	protected Model addSpaceHeater(String heatState) {
	   return addOnlineDevice(ImmutableMap.<String, Object>of(
         SpaceHeaterCapability.ATTR_HEATSTATE, heatState), SpaceHeaterCapability.NAMESPACE);
	}
	
	protected Model addThermostat(String hvacMode) {
	   return addOnlineDevice(ImmutableMap.<String, Object>of(
         ThermostatCapability.ATTR_HVACMODE, hvacMode), ThermostatCapability.NAMESPACE
         );
	}


}

