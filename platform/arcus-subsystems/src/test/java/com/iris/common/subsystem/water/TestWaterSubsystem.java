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
package com.iris.common.subsystem.water;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ClasspathDefinitionRegistry;
import com.iris.messages.capability.EcowaterWaterSoftenerCapability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.capability.ValveCapability;
import com.iris.messages.capability.WaterSubsystemCapability;
import com.iris.messages.capability.WaterSubsystemCapability.ContinuousWaterUseEvent;
import com.iris.messages.capability.WaterSubsystemCapability.ExcessiveWaterUseEvent;
import com.iris.messages.capability.WaterSubsystemCapability.LowSaltEvent;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.EcowaterWaterSoftenerModel;
import com.iris.messages.model.dev.WaterSoftenerModel;
import com.iris.messages.model.subs.WaterSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;

public class TestWaterSubsystem extends SubsystemTestCase<WaterSubsystemModel> {

   protected WaterSubsystem subsystem = new WaterSubsystem();

   private Model testValve1;
   private Model testWaterHeater1;
   private Model testWaterSoftener1;
   private Model testEcoWater1;
   

   
   @Override
   protected WaterSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, WaterSubsystemCapability.NAMESPACE);
      return new WaterSubsystemModel(new SimpleModel(attributes));
   }

   protected void start() {
      subsystem.onEvent(SubsystemLifecycleEvent.added(context.model().getAddress()), context);
      subsystem.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);
      store.addListener(new Listener<ModelEvent>() {
         @Override
         public void onEvent(ModelEvent event) {
            subsystem.onEvent(event, context);
         }
      });
   }
   
   @Before
   public void setup(){
      store.addModel(accountModel.toMap());
      store.addModel(placeModel.toMap());
	   subsystem.setDefinitionRegistry(ClasspathDefinitionRegistry.instance());
	   testValve1 = new SimpleModel(ModelFixtures.createWaterValveAttributes());
	   testValve1.setAttribute(ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_OPEN);

	   testWaterHeater1 = new SimpleModel(ModelFixtures.createWaterHeaterAttributes());

	   testWaterSoftener1 = new SimpleModel(ModelFixtures.createWaterSoftenerAttributes());
	   testEcoWater1 = new SimpleModel(ModelFixtures.createEcoWaterWaterSoftenerAttributes());

	   store.addModel(testValve1.toMap());
	   store.addModel(testWaterHeater1.toMap());
	   store.addModel(testWaterSoftener1.toMap());
	   store.addModel(testEcoWater1.toMap());
	   store.commit();
   }
   
   @Test
   public void testWaterDevicesAdded() {
      start();

      assertTrue(WaterSubsystemModel.getAvailable(context.model()));
      Set<String> waterDevices = WaterSubsystemModel.getWaterDevices(context.model());
      assertEquals(3, waterDevices.size());
      verifyContainAddresses(waterDevices, testValve1, testWaterHeater1, testWaterSoftener1);      
      
      //Add 3 more water devices, one for each type.
      SimpleModel testValve2 = new SimpleModel(ModelFixtures.createWaterValveAttributes());
	  SimpleModel testWaterHeater2 = new SimpleModel(ModelFixtures.createWaterHeaterAttributes());
	  SimpleModel testWaterSoftener2 = new SimpleModel(ModelFixtures.createWaterSoftenerAttributes());
	  store.addModel(testValve2.toMap());
	  store.addModel(testWaterHeater2.toMap());
	  store.addModel(testWaterSoftener2.toMap());
	  
	  waterDevices = WaterSubsystemModel.getWaterDevices(context.model());
      assertEquals(6, waterDevices.size());
      verifyContainAddresses(waterDevices, testValve1, testWaterHeater1, testWaterSoftener1, testValve2, testWaterHeater2, testWaterSoftener2); 
	  
   }
   
   
   private void verifyContainAddresses(Set<String> actualSet, Model... expectedModelList) {
	   for(Model curModel: expectedModelList) {
		   assertTrue(actualSet.contains(curModel.getAddress().getRepresentation()));
	   }
   }
   
   @Test
   public void testWaterDevicesRemoved() {
      start();
      assertTrue(WaterSubsystemModel.getAvailable(context.model()));
      //1. Remove softener, and make sure it is no longer in the list
      store.removeModel(testWaterSoftener1.getAddress());      
      Set<String> waterDevices = WaterSubsystemModel.getWaterDevices(context.model());
      assertEquals(2, waterDevices.size());
      verifyContainAddresses(waterDevices, testValve1, testWaterHeater1);      
      assertFalse(waterDevices.contains(testWaterSoftener1.getAddress().getRepresentation()));
      assertTrue(WaterSubsystemModel.getAvailable(context.model()));
      
      //2. Remove valve
      store.removeModel(testValve1.getAddress());
      waterDevices = WaterSubsystemModel.getWaterDevices(context.model());
      assertEquals(1, waterDevices.size());
      verifyContainAddresses(waterDevices, testWaterHeater1); 
      assertTrue(WaterSubsystemModel.getAvailable(context.model()));
      
      //3. Remove heater
      store.removeModel(testWaterHeater1.getAddress());
      waterDevices = WaterSubsystemModel.getWaterDevices(context.model());
      assertEquals(0, waterDevices.size());
      assertFalse(WaterSubsystemModel.getAvailable(context.model()));
      
      
      
   }
   
   @Test
   public void testClosedValveAdded() {
      start();
      //1. Start out no closeValves
      Set<String> closedValves = WaterSubsystemModel.getClosedWaterValves(context.model());      
      assertEquals(0, closedValves.size());
     
      //2. Existing valve state change to CLOSED.
      closeWaterValve(testValve1);
      closedValves = WaterSubsystemModel.getClosedWaterValves(context.model());      
      assertEquals(1, closedValves.size());
      verifyContainAddresses(closedValves, testValve1);
      
      //3. Add a new CLOSED valve
      SimpleModel testValve2 = new SimpleModel(ModelFixtures.createWaterValveAttributes());
	  testValve2.setAttribute(ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_CLOSED);
	  store.addModel(testValve2.toMap());
      
	  closedValves = WaterSubsystemModel.getClosedWaterValves(context.model());      
      assertEquals(2, closedValves.size());
      verifyContainAddresses(closedValves, testValve1, testValve2);
   }
   
   @Test
   public void testClosedValveRemoved() {
      start();
      //1. Start out no closeValves
      Set<String> closedValves = WaterSubsystemModel.getClosedWaterValves(context.model());      
      assertEquals(0, closedValves.size());
     
      //2. Existing valve state change to CLOSED.
      closeWaterValve(testValve1);
      closedValves = WaterSubsystemModel.getClosedWaterValves(context.model());      
      assertEquals(1, closedValves.size());
      verifyContainAddresses(closedValves, testValve1);
      
      //3. Existing valve state change to OPEN.
      openWaterValve(testValve1);
      closedValves = WaterSubsystemModel.getClosedWaterValves(context.model());      
      assertEquals(0, closedValves.size());
      
      //4. Add a new CLOSED water valve
      SimpleModel testValve2 = new SimpleModel(ModelFixtures.createWaterValveAttributes());
	  testValve2.setAttribute(ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_CLOSED);
	  store.addModel(testValve2.toMap());
	  closedValves = WaterSubsystemModel.getClosedWaterValves(context.model());      
      assertEquals(1, closedValves.size());
      verifyContainAddresses(closedValves, testValve2);
      
      //5. Remove the new CLOSED water valve
      store.removeModel(testValve2.getAddress());
      closedValves = WaterSubsystemModel.getClosedWaterValves(context.model());      
      assertEquals(0, closedValves.size());
   }
   
   @Test
   public void testPrimaryWaterHeater() {
	   start();
	   //1. Test one water heater in the system
	   String primaryWaterHeater = WaterSubsystemModel.getPrimaryWaterHeater(context.model());
	   assertEquals(testWaterHeater1.getAddress().getRepresentation(), primaryWaterHeater);
	   
	   
	   //2. Add a 2nd water heater, make sure primaryWaterHeater does not change
	   SimpleModel testWaterHeater2 = new SimpleModel(ModelFixtures.createWaterHeaterAttributes());
	   store.addModel(testWaterHeater2.toMap());
	   primaryWaterHeater = WaterSubsystemModel.getPrimaryWaterHeater(context.model());
	   assertEquals(testWaterHeater1.getAddress().getRepresentation(), primaryWaterHeater);
	   
	   
	   //3. Remove the primary water heater, make sure primaryWaterHeater changes to the 2nd water heater
	   store.removeModel(testWaterHeater1.getAddress());
	   primaryWaterHeater = WaterSubsystemModel.getPrimaryWaterHeater(context.model());
	   assertEquals(testWaterHeater2.getAddress().getRepresentation(), primaryWaterHeater);
	   
	   //4. Remove the 2nd water heater, make sure primaryWaterHeater is not set anymore
	   store.removeModel(testWaterHeater2.getAddress());
	   primaryWaterHeater = WaterSubsystemModel.getPrimaryWaterHeater(context.model());
	   assertTrue(StringUtils.isEmpty(primaryWaterHeater));
	   
   }
   
   
   @Test
   public void testPrimaryWaterSoftener() {
	   start();
	   //1. Test one water softener in the system
	   String primaryWaterSoftener = WaterSubsystemModel.getPrimaryWaterSoftener(context.model());
	   assertEquals(testWaterSoftener1.getAddress().getRepresentation(), primaryWaterSoftener);
	   
	   
	   //2. Add a 2nd water softener, make sure primaryWaterSoftener does not change
	   SimpleModel testWaterSoftener2 = new SimpleModel(ModelFixtures.createWaterSoftenerAttributes());
	   store.addModel(testWaterSoftener2.toMap());
	   primaryWaterSoftener = WaterSubsystemModel.getPrimaryWaterSoftener(context.model());
	   assertEquals(testWaterSoftener1.getAddress().getRepresentation(), primaryWaterSoftener);
	   
	   
	   //3. Remove the primary water softener, make sure primaryWaterSoftener changes to the 2nd water softener
	   store.removeModel(testWaterSoftener1.getAddress());
	   primaryWaterSoftener = WaterSubsystemModel.getPrimaryWaterSoftener(context.model());
	   assertEquals(testWaterSoftener2.getAddress().getRepresentation(), primaryWaterSoftener);
	   
	   //4. Remove the 2nd water softener, make sure primaryWaterSoftener is not set anymore
	   store.removeModel(testWaterSoftener2.getAddress());
	   primaryWaterSoftener = WaterSubsystemModel.getPrimaryWaterSoftener(context.model());
	   assertTrue(StringUtils.isEmpty(primaryWaterSoftener));
	   
   }
   
   @Test
   public void testSetPrimaryWaterSoftener() {
	   start();
	   //1. Test one water softener in the system
	   String primaryWaterSoftener = WaterSubsystemModel.getPrimaryWaterSoftener(context.model());
	   assertEquals(testWaterSoftener1.getAddress().getRepresentation(), primaryWaterSoftener);
	   
	   
	   //2. Set the primary to an invalid address
	   SimpleModel testWaterSoftener2 = new SimpleModel(ModelFixtures.createWaterSoftenerAttributes());	   
	   MessageBody response = setAttributes(ImmutableMap.<String, Object>of(WaterSubsystemCapability.ATTR_PRIMARYWATERSOFTENER, testWaterSoftener2.getAddress().getRepresentation()));
	   //should return error
	   assertEquals(Capability.EVENT_SET_ATTRIBUTES_ERROR, response.getMessageType());   	   	   
	   primaryWaterSoftener = WaterSubsystemModel.getPrimaryWaterSoftener(context.model());
	   assertEquals(testWaterSoftener1.getAddress().getRepresentation(), primaryWaterSoftener);
	   
	   //3. Add the 2nd water softener.  Then set the primary, and this should be OK
	   store.addModel(testWaterSoftener2.toMap());
	   response = setAttributes(ImmutableMap.<String, Object>of(WaterSubsystemCapability.ATTR_PRIMARYWATERSOFTENER, testWaterSoftener2.getAddress().getRepresentation()));
	   //WaterSubsystemModel.setPrimaryWaterSoftener(context.model(), testWaterSoftener2.getAddress().getRepresentation());
	   primaryWaterSoftener = WaterSubsystemModel.getPrimaryWaterSoftener(context.model());
	   assertEquals(testWaterSoftener2.getAddress().getRepresentation(), primaryWaterSoftener);
	   
	   //4. Try to set the primary to a device address that is not water softener
	   response = setAttributes(ImmutableMap.<String, Object>of(WaterSubsystemCapability.ATTR_PRIMARYWATERSOFTENER, testWaterHeater1.getAddress().getRepresentation()));
	   //should return error
	   assertEquals(Capability.EVENT_SET_ATTRIBUTES_ERROR, response.getMessageType()); 
	   primaryWaterSoftener = WaterSubsystemModel.getPrimaryWaterSoftener(context.model());
	   assertEquals(testWaterSoftener2.getAddress().getRepresentation(), primaryWaterSoftener);
	   
   }
   
   @Test
   public void testSetPrimaryWaterHeater() {
	   start();
	   //1. Test one water softener in the system
	   String primaryWaterHeater = WaterSubsystemModel.getPrimaryWaterHeater(context.model());
	   assertEquals(testWaterHeater1.getAddress().getRepresentation(), primaryWaterHeater);
	   
	   
	   //2. Set the primary to an invalid address
	   SimpleModel testWaterHeater2 = new SimpleModel(ModelFixtures.createWaterHeaterAttributes());	   
	   MessageBody response = setAttributes(ImmutableMap.<String, Object>of(WaterSubsystemCapability.ATTR_PRIMARYWATERHEATER, testWaterHeater2.getAddress().getRepresentation()));
	   //should return error
	   assertEquals(Capability.EVENT_SET_ATTRIBUTES_ERROR, response.getMessageType());   	   	   
	   primaryWaterHeater = WaterSubsystemModel.getPrimaryWaterHeater(context.model());
	   assertEquals(testWaterHeater1.getAddress().getRepresentation(), primaryWaterHeater);
	   
	   //3. Add the 2nd water softener.  Then set the primary, and this should be OK
	   store.addModel(testWaterHeater2.toMap());
	   response = setAttributes(ImmutableMap.<String, Object>of(WaterSubsystemCapability.ATTR_PRIMARYWATERHEATER, testWaterHeater2.getAddress().getRepresentation()));
	   primaryWaterHeater = WaterSubsystemModel.getPrimaryWaterHeater(context.model());
	   assertEquals(testWaterHeater2.getAddress().getRepresentation(), primaryWaterHeater);
	   
	   //4. Try to set the primary to a device address that is not water heater
	   response = setAttributes(ImmutableMap.<String, Object>of(WaterSubsystemCapability.ATTR_PRIMARYWATERHEATER, testWaterSoftener1.getAddress().getRepresentation()));
	   //should return error
	   assertEquals(Capability.EVENT_SET_ATTRIBUTES_ERROR, response.getMessageType()); 
	   primaryWaterHeater = WaterSubsystemModel.getPrimaryWaterHeater(context.model());
	   assertEquals(testWaterHeater2.getAddress().getRepresentation(), primaryWaterHeater);
	   
   }
   
   @Test
   public void testContinuousWaterUse() {
      start();
      
      Set<String> continuousWaterUseDevices = WaterSubsystemModel.getContinuousWaterUseDevices(context.model());
      assertEquals(0, continuousWaterUseDevices.size());
      
      triggerContinuousWaterUse(testEcoWater1, true, 1.2, 60);  //test an existing device
      continuousWaterUseDevices = WaterSubsystemModel.getContinuousWaterUseDevices(context.model());
      assertContinuousWaterUseDevices(testEcoWater1);
      assertEmitContinuousWaterUseEvent(testEcoWater1.getAddress(), 1.2, 60);
      assertNotificationSend(testEcoWater1, NotificationsUtil.ContinuousWaterUse.KEY);
      
      
      //add a new one
      SimpleModel testEcoWater2 = new SimpleModel(ModelFixtures.createEcoWaterWaterSoftenerAttributes());
      EcowaterWaterSoftenerModel.setAlertOnContinuousUse(testEcoWater2, true);
      EcowaterWaterSoftenerModel.setContinuousUse(testEcoWater2, true);
      EcowaterWaterSoftenerModel.setContinuousDuration(testEcoWater2, 80);
      EcowaterWaterSoftenerModel.setContinuousRate(testEcoWater2, 2.4);
      store.addModel(testEcoWater2.toMap());   
      //assertContinuousWaterUseDevices(testEcoWater2);
      assertContinuousWaterUseDevices(testEcoWater1, testEcoWater2);
      assertEmitContinuousWaterUseEvent(testEcoWater2.getAddress(), 2.4, 80);
      
      //set one to not continuous use
      SimpleModel testEcoWater3 = new SimpleModel(ModelFixtures.createEcoWaterWaterSoftenerAttributes());
      EcowaterWaterSoftenerModel.setAlertOnContinuousUse(testEcoWater3, true);
      EcowaterWaterSoftenerModel.setContinuousUse(testEcoWater3, true);
      EcowaterWaterSoftenerModel.setContinuousDuration(testEcoWater3, 90);
      EcowaterWaterSoftenerModel.setContinuousRate(testEcoWater3, 3.4);
      store.addModel(testEcoWater3.toMap());   
      assertContinuousWaterUseDevices(testEcoWater1, testEcoWater2, testEcoWater3);
      assertEmitContinuousWaterUseEvent(testEcoWater3.getAddress(), 3.4, 90);
      
      EcowaterWaterSoftenerModel.setContinuousUse(testEcoWater3, false);
      store.updateModel(testEcoWater3.getAddress(), testEcoWater3.toMap());
      assertContinuousWaterUseDevices(testEcoWater1, testEcoWater2);
      
      
      EcowaterWaterSoftenerModel.setAlertOnContinuousUse(testEcoWater2, false);
      store.updateModel(testEcoWater2.getAddress(), testEcoWater2.toMap());
      assertContinuousWaterUseDevices(testEcoWater1);
      
      
   }
   
   @Test
   public void testLowSaltLevel() throws Exception {
      start();
      
      Set<String> lowSaltDevices = WaterSubsystemModel.getLowSaltDevices(context.model());
      assertEquals(0, lowSaltDevices.size());
      
      WaterSoftenerModel.setCurrentSaltLevel(testWaterSoftener1, WaterSubsystemPredicates.LOW_SALT_THRESHOLD-1);
      store.updateModel(testWaterSoftener1.getAddress(), testWaterSoftener1.toMap());
      assertLowSaltDevices(testWaterSoftener1);
      assertEmitLowSaltEvent(testWaterSoftener1.getAddress());
      assertContainsRequestMessageWithAttrs(NotifyRequest.NAME, ImmutableMap.<String, Object>of(
         NotifyRequest.ATTR_MSGKEY, NotificationsUtil.WaterSoftenerLowSalt.KEY,
         NotifyRequest.ATTR_PRIORITY, NotifyRequest.PRIORITY_LOW));  //test notification
      
      WaterSoftenerModel.setCurrentSaltLevel(testWaterSoftener1, WaterSubsystemPredicates.LOW_SALT_THRESHOLD+1);
      store.updateModel(testWaterSoftener1.getAddress(), testWaterSoftener1.toMap());
      assertLowSaltDevices();
      
      SimpleModel testWaterSoftener2 = new SimpleModel(ModelFixtures.createWaterSoftenerAttributes());
      WaterSoftenerModel.setCurrentSaltLevel(testWaterSoftener2, WaterSubsystemPredicates.LOW_SALT_THRESHOLD-1);
      store.addModel(testWaterSoftener2.toMap());  //add one device low salt
      assertLowSaltDevices(testWaterSoftener2);
      assertEmitLowSaltEvent(testWaterSoftener2.getAddress());
      
      WaterSoftenerModel.setSaltLevelEnabled(testWaterSoftener2, Boolean.FALSE);   //test the flag
      store.updateModel(testWaterSoftener2.getAddress(), testWaterSoftener2.toMap());
      assertLowSaltDevices();
   }
   

   private void assertNotificationSend(Model testEcoWater12, String msgKey)
   {
      assertContainsRequestMessageWithAttrs(NotifyRequest.NAME, ImmutableMap.<String, Object>of(
            NotifyRequest.ATTR_MSGKEY, msgKey,
            NotifyRequest.ATTR_PRIORITY, NotifyRequest.PRIORITY_MEDIUM));
      assertContainsRequestMessageWithAttrs(NotifyRequest.NAME, ImmutableMap.<String, Object>of(
         NotifyRequest.ATTR_MSGKEY, msgKey,
         NotifyRequest.ATTR_PRIORITY, NotifyRequest.PRIORITY_LOW));
   }

   private void assertEmitContinuousWaterUseEvent(Address sensorAddress, double flowRate, int durationSec)
   {
      assertContainsBroadcastEventWithAttrs(WaterSubsystemCapability.ContinuousWaterUseEvent.NAME, ImmutableMap.<String, Object>of(
         ContinuousWaterUseEvent.ATTR_DURATIONSEC, durationSec,
         ContinuousWaterUseEvent.ATTR_FLOWRATE, flowRate,
         ContinuousWaterUseEvent.ATTR_SENSOR, sensorAddress.getRepresentation()));
      
   }
   
   private void assertEmitExessiveWaterUseEvent(Address sensorAddress)
   {
      assertContainsBroadcastEventWithAttrs(WaterSubsystemCapability.ExcessiveWaterUseEvent.NAME, ImmutableMap.<String, Object>of(
         ExcessiveWaterUseEvent.ATTR_SENSOR, sensorAddress.getRepresentation()));
   }

   private void assertEmitLowSaltEvent(Address sensorAddress)
   {
      assertContainsBroadcastEventWithAttrs(WaterSubsystemCapability.LowSaltEvent.NAME, ImmutableMap.<String, Object>of(
         LowSaltEvent.ATTR_SENSOR, sensorAddress.getRepresentation()));
   }

   @Test
   public void testExessiveWaterUse() {
      start();
      
      Set<String> exessiveWaterUseDevices = WaterSubsystemModel.getExcessiveWaterUseDevices(context.model());
      assertEquals(0, exessiveWaterUseDevices.size());
      
      triggerExessiveWaterUse(testEcoWater1, true);  //test an existing device
      exessiveWaterUseDevices = WaterSubsystemModel.getExcessiveWaterUseDevices(context.model());
      assertExessiveWaterUseDevices(testEcoWater1);
      assertEmitExessiveWaterUseEvent(testEcoWater1.getAddress());
      assertNotificationSend(testEcoWater1, NotificationsUtil.ExcessiveWaterUse.KEY);
      
      //add a new one
      SimpleModel testEcoWater2 = new SimpleModel(ModelFixtures.createEcoWaterWaterSoftenerAttributes());
      EcowaterWaterSoftenerModel.setAlertOnExcessiveUse(testEcoWater2, true);
      EcowaterWaterSoftenerModel.setExcessiveUse(testEcoWater2, true);      
      store.addModel(testEcoWater2.toMap());   
      assertExessiveWaterUseDevices(testEcoWater1, testEcoWater2);
      assertEmitExessiveWaterUseEvent(testEcoWater2.getAddress());
      
      //set one to not continuous use
      SimpleModel testEcoWater3 = new SimpleModel(ModelFixtures.createEcoWaterWaterSoftenerAttributes());
      EcowaterWaterSoftenerModel.setAlertOnExcessiveUse(testEcoWater3, true);
      EcowaterWaterSoftenerModel.setExcessiveUse(testEcoWater3, true);      
      store.addModel(testEcoWater3.toMap());   
      assertExessiveWaterUseDevices(testEcoWater1, testEcoWater2, testEcoWater3);
      assertEmitExessiveWaterUseEvent(testEcoWater3.getAddress());
      
      EcowaterWaterSoftenerModel.setExcessiveUse(testEcoWater3, false);
      store.updateModel(testEcoWater3.getAddress(), testEcoWater3.toMap());
      assertExessiveWaterUseDevices(testEcoWater1, testEcoWater2);
      
      
      EcowaterWaterSoftenerModel.setAlertOnExcessiveUse(testEcoWater2, false);
      store.updateModel(testEcoWater2.getAddress(), testEcoWater2.toMap());
      assertExessiveWaterUseDevices(testEcoWater1);      
      
   }
   
   private void assertExessiveWaterUseDevices(Model...models)
   {
      Set<String> actualDevices = WaterSubsystemModel.getExcessiveWaterUseDevices(context.model());
      assertDevicesMatches(actualDevices, models);           
   }

   private void assertContinuousWaterUseDevices(Model...models) {
      Set<String> continuousWaterUseDevices = WaterSubsystemModel.getContinuousWaterUseDevices(context.model());
      assertDevicesMatches(continuousWaterUseDevices, models);         
   }
   
   private void assertLowSaltDevices(Model...models) {
      Set<String> lowSaltDevices = WaterSubsystemModel.getLowSaltDevices(context.model());
      assertDevicesMatches(lowSaltDevices, models);         
   }
   
   private void assertDevicesMatches(Set<String> actualDeviceAddresses, Model...expectedModels) {
      if(expectedModels != null && expectedModels.length > 0) {
         verifyContainAddresses(actualDeviceAddresses, expectedModels);
         assertEquals(expectedModels.length, actualDeviceAddresses.size());
      }else{
         assertEquals(0, actualDeviceAddresses.size());
      }  
   }
   
   private void triggerContinuousWaterUse(Model model, boolean alertOnFlag, double rate, int duration) {
      model.setAttribute(EcowaterWaterSoftenerCapability.ATTR_ALERTONCONTINUOUSUSE, alertOnFlag);
      model.setAttribute(EcowaterWaterSoftenerCapability.ATTR_CONTINUOUSUSE, true);
      model.setAttribute(EcowaterWaterSoftenerCapability.ATTR_CONTINUOUSRATE, rate);
      model.setAttribute(EcowaterWaterSoftenerCapability.ATTR_CONTINUOUSDURATION, duration);
      store.updateModel(model.getAddress(), model.toMap());
   }
   
   private void triggerExessiveWaterUse(Model model, boolean alertOnFlag) {
      model.setAttribute(EcowaterWaterSoftenerCapability.ATTR_ALERTONEXCESSIVEUSE, alertOnFlag);
      model.setAttribute(EcowaterWaterSoftenerCapability.ATTR_EXCESSIVEUSE, true);      
      store.updateModel(model.getAddress(), model.toMap());
   }
   
   private void closeWaterValve(Model model){
	   changeWaterValveState(model, ValveCapability.VALVESTATE_CLOSED);
   }
   
   private void openWaterValve(Model model){
	   changeWaterValveState(model, ValveCapability.VALVESTATE_OPEN);
   }
   
   private void changeWaterValveState(Model model, String state) {
	   model.setAttribute(ValveCapability.ATTR_VALVESTATE, state);
	   store.updateModel(model.getAddress(), model.toMap());
   }
   
   
   protected MessageBody setAttributes(Map<String, Object> attributes) {
      MessageBody request = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes);
      PlatformMessage msg = PlatformMessage.buildRequest(request, Address.clientAddress("android", "1"), Address.platformService(placeId, WaterSubsystemCapability.NAMESPACE))
         .withPlaceId(placeId)
         .create();
      MessageBody response = subsystem.setAttributes(msg, context);
      return response;
   }
   
   
  
   


}

