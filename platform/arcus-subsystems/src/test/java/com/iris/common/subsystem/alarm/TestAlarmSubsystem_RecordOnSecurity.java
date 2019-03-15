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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.CameraCapability;
import com.iris.messages.capability.CarbonMonoxideCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.model.test.ModelFixtures.DeviceBuilder;
import com.iris.messages.service.VideoService.StartRecordingRequest;

@RunWith(Parameterized.class)
public class TestAlarmSubsystem_RecordOnSecurity extends PlatformAlarmSubsystemTestCase {
	private Model contact;
   private Model smoke;
   private Model co;
   private Model camera;
   private Model camera2;
   private String serviceLevel;
   private Boolean recordingSupportedForServiceLevel;
   private String alarm;
   private Boolean recordingSupportedForAlarm;
   
   @Parameters(name="serviceLevel:{0}, supportedByServiceLevel:{1}, alarm:{2}, alarmSupported:{3}")
   public static Collection<Object[]> cases() {
      return Arrays.asList(new Object[][] {
            { PlaceCapability.SERVICELEVEL_PREMIUM, Boolean.TRUE, SecurityAlarm.NAME, Boolean.TRUE },
            { PlaceCapability.SERVICELEVEL_PREMIUM_ANNUAL, Boolean.TRUE, SecurityAlarm.NAME, Boolean.TRUE },
            { PlaceCapability.SERVICELEVEL_PREMIUM_FREE, Boolean.TRUE , SecurityAlarm.NAME, Boolean.TRUE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON, Boolean.TRUE , SecurityAlarm.NAME, Boolean.TRUE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_ANNUAL, Boolean.TRUE , SecurityAlarm.NAME, Boolean.TRUE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_FREE, Boolean.TRUE , SecurityAlarm.NAME, Boolean.TRUE},
            { PlaceCapability.SERVICELEVEL_BASIC, Boolean.FALSE , SecurityAlarm.NAME, Boolean.FALSE},
            
            { PlaceCapability.SERVICELEVEL_PREMIUM, Boolean.TRUE, PanicAlarm.NAME, Boolean.TRUE },
            { PlaceCapability.SERVICELEVEL_PREMIUM_ANNUAL, Boolean.TRUE, PanicAlarm.NAME, Boolean.TRUE },
            { PlaceCapability.SERVICELEVEL_PREMIUM_FREE, Boolean.TRUE , PanicAlarm.NAME, Boolean.TRUE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON, Boolean.TRUE , PanicAlarm.NAME, Boolean.TRUE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_ANNUAL, Boolean.TRUE , PanicAlarm.NAME, Boolean.TRUE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_FREE, Boolean.TRUE , PanicAlarm.NAME, Boolean.TRUE},
            { PlaceCapability.SERVICELEVEL_BASIC, Boolean.FALSE , PanicAlarm.NAME, Boolean.FALSE},
            
            { PlaceCapability.SERVICELEVEL_PREMIUM, Boolean.TRUE, SmokeAlarm.NAME, Boolean.FALSE },
            { PlaceCapability.SERVICELEVEL_PREMIUM_ANNUAL, Boolean.TRUE, SmokeAlarm.NAME, Boolean.FALSE },
            { PlaceCapability.SERVICELEVEL_PREMIUM_FREE, Boolean.TRUE , SmokeAlarm.NAME, Boolean.FALSE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON, Boolean.TRUE , SmokeAlarm.NAME, Boolean.FALSE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_ANNUAL, Boolean.TRUE , SmokeAlarm.NAME, Boolean.FALSE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_FREE, Boolean.TRUE , SmokeAlarm.NAME, Boolean.FALSE},
            { PlaceCapability.SERVICELEVEL_BASIC, Boolean.FALSE , SmokeAlarm.NAME, Boolean.FALSE},

            { PlaceCapability.SERVICELEVEL_PREMIUM, Boolean.TRUE, CarbonMonoxideAlarm.NAME, Boolean.FALSE },
            { PlaceCapability.SERVICELEVEL_PREMIUM_ANNUAL, Boolean.TRUE, CarbonMonoxideAlarm.NAME, Boolean.FALSE },
            { PlaceCapability.SERVICELEVEL_PREMIUM_FREE, Boolean.TRUE , CarbonMonoxideAlarm.NAME, Boolean.FALSE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON, Boolean.TRUE , CarbonMonoxideAlarm.NAME, Boolean.FALSE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_ANNUAL, Boolean.TRUE , CarbonMonoxideAlarm.NAME, Boolean.FALSE},
            { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_FREE, Boolean.TRUE , CarbonMonoxideAlarm.NAME, Boolean.FALSE},
            { PlaceCapability.SERVICELEVEL_BASIC, Boolean.FALSE , CarbonMonoxideAlarm.NAME, Boolean.FALSE}

   });
   }
   
   public TestAlarmSubsystem_RecordOnSecurity(String serviceLevel, Boolean recordingSupportedForServiceLevel, String alarm, Boolean recordingSupportedForAlarm) {
      this.serviceLevel = serviceLevel;
      this.recordingSupportedForServiceLevel = recordingSupportedForServiceLevel;
      this.alarm = alarm;
      this.recordingSupportedForAlarm = recordingSupportedForAlarm;
   }
	
	@Before
   public void createDevices() {
      // enable the alarms
      contact = addContactDevice();
      smoke = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
      co = addCODevice(true, CarbonMonoxideCapability.CO_SAFE);
      SecurityAlarmModeModel.setDevices(AlarmSubsystemCapability.SECURITYMODE_ON, securitySubsystem, ImmutableSet.of(contact.getAddress().getRepresentation()));
      
      PlaceModel.setServiceLevel(placeModel, serviceLevel);
      store.addModel(placeModel.toMap());
	}
	

	protected void start() throws Exception {
		init(subsystem);
		requests.reset();
	}
	
	@Test
	public void testRecordingDurationSec() throws Exception {
	   start();
	   
	   //Note the current min max is set at (30, 1200)
	   doSetRecordingDurationSec(30, true);	   
	   doSetRecordingDurationSec(60, true);	   
	   doSetRecordingDurationSec(300, true);
	   doSetRecordingDurationSec(1200, true);
	   
	   doSetRecordingDurationSec(20, false);	   
	   doSetRecordingDurationSec(1201, false);
	   doSetRecordingDurationSec(29, false);
	}
	
	private void doSetRecordingDurationSec(Integer value, boolean success) {
	   
	   try{
	      subsystem.setAttribute(AlarmSubsystemCapability.ATTR_RECORDINGDURATIONSEC, value, context);
	      if(!success) {
	         fail("Should have failed to doSetRecordingDurationSec for value "+value);
	      }else{
	         assertEquals(value, context.model().getRecordingDurationSec());
	      }
	   }catch(Exception e) {
	      if(success) {
	         e.printStackTrace();
	         fail("fail to doSetRecordingDurationSec for value "+value);
	      }
	   }
	}
	
	@Test
   public void testSubystemStartNoCapableDevices() throws Exception {
      start();
      
      assertFalse(context.model().getRecordingSupported());
      assertTrue(context.model().getRecordOnSecurity());
      assertEquals(RecordOnSecurityAdapter.RECORDING_DURATION_DEFAULT, context.model().getRecordingDurationSec());     
   }
	
	
	@Test
   public void testSubystemAddOneCamera() throws Exception {
	   
	   replay();
	   
      start();     
      //Test FanShutoffSupported flag based on capable devices add and remove
      camera = addCamera();
      assertEquals(recordingSupportedForServiceLevel, context.model().getRecordingSupported());    
            
      removeModel(camera);
      assertFalse(context.model().getRecordingSupported());
            
      verify();
   }
	
	@Test
	public void testTriggerAlert() throws Exception {
	   setExitDelay(AlarmSubsystemCapability.SECURITYMODE_ON, true, 0);
	   setEntryDelay(AlarmSubsystemCapability.SECURITYMODE_ON, 0);
	   expectAddAlert(alarm);
	   replay();
      
      start();     
      //Test FanShutoffSupported flag based on capable devices add and remove
      camera = addCamera();
      camera2 = addCamera();
      arm(AlarmSubsystemCapability.SECURITYMODE_ON);
      
      switch(alarm) {
         case SecurityAlarm.NAME: 
            trigger(contact); 
            break;
         case PanicAlarm.NAME:
            panic();
            break;
         case SmokeAlarm.NAME:
            trigger(smoke);
            break;
         case CarbonMonoxideAlarm.NAME:
            trigger(co);
            break;
      }
      
      if(recordingSupportedForServiceLevel && recordingSupportedForAlarm) {
         assertContainsRequestMessageWithAttrs(StartRecordingRequest.NAME, createRecordingRequestFor(camera));
         assertContainsRequestMessageWithAttrs(StartRecordingRequest.NAME, createRecordingRequestFor(camera2));
      }else{
         assertNotContainsRequestMessageWithAttrs(StartRecordingRequest.NAME, createRecordingRequestFor(camera));
         assertNotContainsRequestMessageWithAttrs(StartRecordingRequest.NAME, createRecordingRequestFor(camera2));
      }
            
      verify();
	}
	
	private Map<String, Object> createRecordingRequestFor(Model cameraModel) {
	   return ImmutableMap.<String, Object>of(
         StartRecordingRequest.ATTR_CAMERAADDRESS, cameraModel.getAddress().getRepresentation(),
         StartRecordingRequest.ATTR_DURATION, context.model().getRecordingDurationSec(),
         StartRecordingRequest.ATTR_PLACEID, placeModel.getId(),
         StartRecordingRequest.ATTR_ACCOUNTID, accountModel.getId());
	}
	
	
   
	protected Model addOnlineDevice(String deviceNameSpace, Map<String, Object> attribs) {
       
	   DeviceBuilder m = ModelFixtures.buildDeviceAttributes(deviceNameSpace);
	   if(attribs != null && !attribs.isEmpty()) {
	      m.putAll(attribs);
	   }
	   m.put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
	   return addModel(m.create());
           
   }
	
	protected Model addCamera() {
	   return addOnlineDevice(CameraCapability.NAMESPACE, ImmutableMap.<String, Object>of());
	}
	
	protected void setExitDelay(String mode, boolean soundsEnabled, int durationSec) {
      SecurityAlarmModeModel.setSoundsEnabled(mode, securitySubsystem, soundsEnabled);
      SecurityAlarmModeModel.setExitDelaySec(mode, securitySubsystem, durationSec);
   }
	
	protected void setEntryDelay(String mode, int durationSec) {
      SecurityAlarmModeModel.setEntranceDelaySec(mode, securitySubsystem, durationSec);
   }

}

