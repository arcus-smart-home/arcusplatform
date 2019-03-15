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
package com.iris.common.subsystem.safety;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.capability.HaloCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.util.TypeMarker;

public class TestHaloPreSmoke extends SafetySubsystemTestCase {

   public static final String HALO_PRESMOKE_ALERT = HaloCapability.ATTR_DEVICESTATE + ":" + HaloCapability.DEVICESTATE_PRE_SMOKE;

   @Test
   public void testOnStarted() {
      startSubsystem();
      SafetySubsystemModel model = context.model();

      Set<String> devices = model.getTotalDevices();

      assertTrue(devices.contains(haloDevice1.getAddress().getRepresentation()));
      assertTrue(devices.contains(haloDevice2.getAddress().getRepresentation()));
      assertTrue(devices.contains(haloDevice3.getAddress().getRepresentation()));
   }

   @Test
   public void testPreSmokeTriggerEvent() {
      startSubsystem();

      SafetySubsystemModel model = context.model();
      
      Model[] models = {haloDevice3,haloDevice1,haloDevice2};
      
      faultDevices(models);
      
      List<String> preSmokeDevices = model.getSmokePreAlertDevices();
      
      /*
       *  Test That They Exist and That Order is Maintained as guaranteed by
       *  the OrderedAddressesAttributeBinder
       */
      
      
      int i = 0;
      for (String address : preSmokeDevices) {
         assertTrue(models[i++].getAddress().getRepresentation().equals(address));
      }
   }

   protected void faultDevices(Model... devices) {
      for (Model device: devices) {
         updateDeviceOnPreSmoke(device);
      }
   }
   
   /*
    * The following method will insert the objects through a listener on the subsystem
    * in response to a model changed event. Doing so will guarantee that these devices
    * are added to a list of pre-smoke alert devices through the OrderedAddressesAttributeBinder
    */
   private void updateDeviceOnPreSmoke(Model device) {
      Map<String, Object> update = ImmutableMap.<String, Object> of(
            HaloCapability.ATTR_DEVICESTATE, HaloCapability.DEVICESTATE_PRE_SMOKE);
      updateModel(device.getAddress(), update);      
   }

   public static Set<String> getPreSmokeHaloDevices(SubsystemContext<SafetySubsystemModel> context) {
      return context.model().getAttribute(TypeMarker.setOf(String.class), HALO_PRESMOKE_ALERT, ImmutableSet.<String> of());
   }

}

