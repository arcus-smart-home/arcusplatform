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
package com.iris.common.subsystem.lightsnswitches;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.LightsNSwitchesSubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.LightsNSwitchesSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;

public class TestLightNSwitchesSubsystem extends SubsystemTestCase<LightsNSwitchesSubsystemModel> {

   protected LightsNSwitchesSubsystem subsystem = new LightsNSwitchesSubsystem();

   private Model testSwitch;
   private Model testDimmer;
   private Model testLight;

   

   
   @Override
   protected LightsNSwitchesSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, LightsNSwitchesSubsystemCapability.NAMESPACE);
      return new LightsNSwitchesSubsystemModel(new SimpleModel(attributes));
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
      testSwitch = new SimpleModel(ModelFixtures.createSwitchAttributes());
      testSwitch.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, "Switch");

      testDimmer = new SimpleModel(ModelFixtures.createSwitchAttributes());
      testDimmer.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, "Dimmer");

      testLight = new SimpleModel(ModelFixtures.createSwitchAttributes());
      testLight.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, "Light");

      store.addModel(testSwitch.toMap());
      store.addModel(testDimmer.toMap());
      store.addModel(testLight.toMap());
   }
   
   @Test
   public void testSwitchOn() {
      start();
      turnOn(testSwitch);
      turnOn(testDimmer);
      turnOn(testLight);

      Map<String,Integer>onCounts=LightsNSwitchesSubsystemModel.getOnDeviceCounts(context.model());
      assertEquals("switches",new Integer(1),onCounts.get("switch"));
      assertEquals("dimemrs", new Integer(1),onCounts.get("dimmer"));
      assertEquals("lights", new Integer(1),onCounts.get("light"));
      
      turnOff(testSwitch);
      onCounts=LightsNSwitchesSubsystemModel.getOnDeviceCounts(context.model());
      assertEquals("switches",new Integer(0),onCounts.get("switch"));
   }
   
   @Test
   public void testAddModel() {
      start();
      Model testSwitch2 = new SimpleModel(ModelFixtures.createSwitchAttributes());
      testSwitch2.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, "Switch");
      testSwitch2.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      store.addModel(testSwitch2.toMap());
      Map<String,Integer>onCounts=LightsNSwitchesSubsystemModel.getOnDeviceCounts(context.model());
      assertEquals("switches count",new Integer(1),onCounts.get("switch"));
   }
   
   @Test
   public void testAvailable() {
      start();
      assertEquals("should be available",true,context.model().getAvailable());
      store.removeModel(testSwitch.getAddress());
      store.removeModel(testDimmer.getAddress());
      store.removeModel(testLight.getAddress());
      assertEquals("should be available",false,context.model().getAvailable());

   }
   
   @Test
   public void testRemoveModel() {
      start();
      turnOn(testSwitch);
      store.removeModel(testSwitch.getAddress());
      Map<String,Integer>onCounts=LightsNSwitchesSubsystemModel.getOnDeviceCounts(context.model());
      assertEquals("switches count",new Integer(0),onCounts.get("switch"));
   }
   
   private void turnOn(Model model){
      model.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      store.updateModel(model.getAddress(), model.toMap());
   }
   private void turnOff(Model model){
      model.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      store.updateModel(model.getAddress(), model.toMap());
   }
}

