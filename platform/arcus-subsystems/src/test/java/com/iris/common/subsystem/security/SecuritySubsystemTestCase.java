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
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ClasspathDefinitionRegistry;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.KeyPadCapability.ArmPressedEvent;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.CallTreeEntry;
import com.iris.util.IrisCollections;

public class SecuritySubsystemTestCase extends SubsystemTestCase<SecuritySubsystemModel> {
   private static final Logger LOGGER = LoggerFactory.getLogger(SecuritySubsystemTestCase.class);

   protected SecuritySubsystemV1 subsystem = new SecuritySubsystemV1();
   
   protected Model glassbreakSensor = null;
   protected Model motionSensor = null;
   protected Model contactSensor = null;
   protected Model offlineSecurityDevice = null;
   protected Model nonSecurityDevice = null;
   protected Model noneGenieMotorizedDoor = null;
   protected Model keyPad = null;
   protected int numOfMotionSensors = 2;

   
   protected Model owner = null;

   protected Model siren = null;
   protected boolean started = false;
   
   @Override
   protected SecuritySubsystemModel createSubsystemModel() {
	   Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, SecuritySubsystemCapability.NAMESPACE);
	      return new SecuritySubsystemModel(new SimpleModel(attributes));
   }
   
   
   @SuppressWarnings("unchecked")
   @Before
   public void setUp() {
      super.setUp();
      started = false;
      subsystem.setDefinitionRegistry(ClasspathDefinitionRegistry.instance());
      
      glassbreakSensor = addModel(SecurityFixtures.createGlassbreakFixture());
      motionSensor = addModel(SecurityFixtures.createMotionFixture());
      contactSensor = addModel(SecurityFixtures.createContactFixture());
      nonSecurityDevice = addModel(ModelFixtures.createSwitchAttributes());
      noneGenieMotorizedDoor = addModel(ModelFixtures.createMotorizedDoorFixture());
      keyPad = addModel(SecurityFixtures.createKeypadFixture());

      offlineSecurityDevice = new SimpleModel(SecurityFixtures.createMotionFixture());
      offlineSecurityDevice.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      addModel(offlineSecurityDevice.toMap());
      
      owner = addModel(ModelFixtures.createPersonAttributes());
      
      siren = addModel(SecurityFixtures.createAlertFixture());
                  
      placeModel.setAttribute(PlaceCapability.ATTR_SERVICELEVEL, ServiceLevel.PREMIUM.name());
      addModel(placeModel.toMap());
            
      accountModel.setAttribute(AccountCapability.ATTR_OWNER, owner.getId());
      addModel(accountModel.toMap());
    }

   @SuppressWarnings("unchecked")
   protected void initModelStore() {
      store.addModel(
            IrisCollections
               .setOf(
                  glassbreakSensor.toMap(), 
                  motionSensor.toMap(),
                  contactSensor.toMap(),
                  placeModel.toMap(),
                  accountModel.toMap(),
                  nonSecurityDevice.toMap(),
                  offlineSecurityDevice.toMap(),
                  siren.toMap(),
                  owner.toMap(),
                  keyPad.toMap()
               )
      );
   }

   protected void immediateArmOn() {
      armOn();
      timeout();
   }
   
   protected MessageBody armOn() {
      MessageBody armResponse = subsystem.arm(securityPlatformMessage(MessageBody.buildMessage("test",new HashMap<String,Object>())),SecuritySubsystemV1.MODE_ON, context);
      return armResponse;
   }
   
   protected void armOnKeypad(Address fromAddress) {
      MessageBody event = ArmPressedEvent.builder().withMode("ON").build();
      subsystem.onKeypadArm(securityPlatformMessage(event, fromAddress),context);
   }
   
   //Arm from the mobile app
   protected void armOnKeypad() {
	   armOnKeypad(clientAddress);
   }
   
   protected MessageBody armPartial() {
      MessageBody armResponse = subsystem.arm(securityPlatformMessage(MessageBody.buildMessage("test",new HashMap<String,Object>())),SecuritySubsystemV1.MODE_PARTIAL, context);
      return armResponse;
   }

   protected MessageBody armBypassed() {
      return armBypassed(SecuritySubsystemV1.MODE_PARTIAL);
   }
   
   protected MessageBody armBypassed(String mode) {
      MessageBody armResponse = subsystem.armBypassed(mode, context, securityPlatformMessage(MessageBody.buildMessage("test",new HashMap<String,Object>())));
      return armResponse;
   }

   protected void disarmSystem(){
	   disarmSystem(clientAddress);
   }
   
   protected void disarmSystem(Address fromAddress){
	  subsystem.disarm(securityPlatformMessage(MessageBody.buildMessage("test",new HashMap<String,Object>()), fromAddress), context);
   }
   
   /**
    * Causes the current state to timeout 
    */
   protected ScheduledEvent timeout() {
      ScheduledEvent event = super.timeout();
      subsystem.onScheduledEvent(event, context);
      return event;
   }
   
   protected ScheduledEvent timeout(String name) {
      ScheduledEvent event = super.timeout(name);
      subsystem.onScheduledEvent(event, context);
      return event;
   }   

   protected void faultDevice(Model device) {
      ModelChangedEvent event = null;
      if (device.supports(GlassCapability.NAMESPACE)) {
         event = ModelChangedEvent.create(device.getAddress(), GlassCapability.ATTR_BREAK, GlassCapability.BREAK_DETECTED, null);
         subsystem.onGlassBreak(event, context);
      }
      else if (device.supports(MotionCapability.NAMESPACE)) {
         event = ModelChangedEvent.create(device.getAddress(), MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED, null);
         subsystem.onMotionChange(event, context);
      }
      else if (device.supports(ContactCapability.NAMESPACE)) {
         event = ModelChangedEvent.create(device.getAddress(), ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED, null);
         subsystem.onContactChange(event, context);
      }
      else if(device.supports(MotorizedDoorCapability.NAMESPACE)) {
          event = ModelChangedEvent.create(device.getAddress(), MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN, null);
          subsystem.onMotorizedDoorStateChange(event, context);    	  
      }
      else {
         throw new IllegalArgumentException("unsupport security fault device");
      }
   }
   
   protected void takeDeviceOffline(Model device){
      device.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      initModelStore();
      ModelChangedEvent connectivyOffline = ModelChangedEvent.create(device.getAddress(), DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE, DeviceConnectionCapability.STATE_ONLINE);
      subsystem.onConnectivityStateChange(connectivyOffline, context);
   }
   
   protected void takeDeviceOnline(Model device){
      device.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      initModelStore();
      ModelChangedEvent connectivyOffline = ModelChangedEvent.create(device.getAddress(), DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE, DeviceConnectionCapability.STATE_OFFLINE);
      subsystem.onConnectivityStateChange(connectivyOffline, context);
   }
   
   protected void clearDevice(Model device) {
      ModelChangedEvent event = null;
      if (device.supports(GlassCapability.NAMESPACE)) {
         event = ModelChangedEvent.create(device.getAddress(), GlassCapability.ATTR_BREAK, GlassCapability.BREAK_SAFE, null);
         subsystem.onGlassBreak(event, context);
      }
      else if (device.supports(MotionCapability.NAMESPACE)) {
         event = ModelChangedEvent.create(device.getAddress(), MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE, null);
         subsystem.onMotionChange(event, context);
      }
      else if (device.supports(ContactCapability.NAMESPACE)) {
         event = ModelChangedEvent.create(device.getAddress(), ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED, null);
         subsystem.onContactChange(event, context);
      }
      else if (device.supports(MotorizedDoorCapability.NAMESPACE)) {
          event = ModelChangedEvent.create(device.getAddress(), MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED, null);
          subsystem.onContactChange(event, context);
       }
      else {
         throw new IllegalArgumentException("unsupport security fault device");
      }
   }

   protected void removeDevice(Model deviceModel) {
      ModelRemovedEvent event = ModelRemovedEvent.create(deviceModel);
      subsystem.onDeviceRemoved(event, context);
   }

   protected void setAttributes(Map<String, Object> attributes) {
      MessageBody request = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes);
      PlatformMessage msg = PlatformMessage.buildRequest(request, Address.clientAddress("android", "1"), Address.platformService(placeId, SecuritySubsystemCapability.NAMESPACE))
         .withPlaceId(placeId)
         .create();
      subsystem.setAttributes(msg, context);
   }

   protected void startSubsystem() {
		subsystem.onEvent(
				SubsystemLifecycleEvent.added(context.model().getAddress()),
				context);
		subsystem.onEvent(
				SubsystemLifecycleEvent.started(context.model().getAddress()),
				context);
		store.addListener(new Listener<ModelEvent>() {
			@Override
			public void onEvent(ModelEvent event) {
				subsystem.onEvent(event, context);
			}
		});
		started = true;   
   }

   protected PlatformMessage securityPlatformMessage(MessageBody body) {
      return securityPlatformMessage(body, clientAddress);
   }
   
   protected PlatformMessage securityPlatformMessage(MessageBody body, Address fromAddress) {
	      PlatformMessage message =
	            PlatformMessage
	                  .request(model.getAddress())
	                  .from(fromAddress)
	                  .withActor(owner.getAddress())
	                  .withPayload(body)
	                  .create();
	      return message;
	   }

   protected boolean addressExists(String address, List<Map<String, Object>> list) {
      boolean found = false;
      for (Map<String, Object> cte : list) {
         if (cte.get(CallTreeEntry.ATTR_PERSON).equals(address)) {
            found = true;
            break;
         }
      }
      return found;
   }
}

