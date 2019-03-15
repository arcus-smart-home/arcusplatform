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
package com.iris.platform.subsystem.placemonitor;

import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.core.template.TemplateService;
import com.iris.messages.capability.BridgeCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.platform.subsystem.placemonitor.pairing.PlacePairingModeHandler;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertHandler;

public class TestPlacePairingModeHandler extends SubsystemTestCase<PlaceMonitorSubsystemModel> {
   
   private PlaceMonitorSubsystem subsystem = null;
   PlacePairingModeHandler handler=null;
   
   Model bridgeDevice1 =null;
   Model bridgeDevice2 =null;

   Map<String, Object> place = ModelFixtures.createPlaceAttributes();

   Model accountModel = null;
   Model owner = null;
   Model hub = null;

   TemplateService templateService = EasyMock.createMock(TemplateService.class);

   @Override
   protected PlaceMonitorSubsystemModel createSubsystemModel() {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, PlaceMonitorSubsystemCapability.NAMESPACE);
      return new PlaceMonitorSubsystemModel(new SimpleModel(attributes));
   }

   @Override
   protected Subsystem<PlaceMonitorSubsystemModel> subsystem() {
      return subsystem;
   }

   protected void reloadStore(){
      store.addModel(place);
      store.addModel(hub.toMap());
      store.addModel(bridgeDevice1.toMap());
      store.addModel(bridgeDevice2.toMap());
      store.addModel(ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).create());
      
   }

   protected void start() {
      reloadStore();
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
   public void init() {
      subsystem = new PlaceMonitorSubsystem(ImmutableMap.<String, PlaceMonitorHandler>of(),new PlaceMonitorNotifications(null), templateService);
      bridgeDevice1 = new SimpleModel(ModelFixtures.buildDeviceAttributes(BridgeCapability.NAMESPACE).create());
      bridgeDevice1.setAttribute(DeviceCapability.ATTR_NAME, "Bridge1");
      bridgeDevice2 = new SimpleModel(ModelFixtures.buildDeviceAttributes(BridgeCapability.NAMESPACE).create());
      bridgeDevice2.setAttribute(DeviceCapability.ATTR_NAME, "Bridge2");
      
      handler = new PlacePairingModeHandler();
      subsystem.handlers=ImmutableMap.<String, PlaceMonitorHandler>of(PlacePairingModeHandler.class.getName(),handler);
      place.put(PlaceCapability.ATTR_ID,placeId.toString());
      
      hub = new SimpleModel(ModelFixtures.createHubAttributes());
   }

   
   @Test
   public void testOnStartAllBridgeAndHubIdle() {
      bridgeDevice1.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_IDLE);
      bridgeDevice2.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_IDLE);
      hub.setAttribute(HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL);
      start();
      assertEquals(PlaceMonitorSubsystemCapability.PAIRINGSTATE_IDLE,context.model().getAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE));
      
   }
   @Test
   public void testOnStartAllBridgeAndHubPairing() {
      bridgeDevice1.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_PAIRING);
      bridgeDevice2.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_PAIRING);
      hub.setAttribute(HubCapability.ATTR_STATE, HubCapability.STATE_PAIRING);
      start();
      assertEquals(PlaceMonitorSubsystemCapability.PAIRINGSTATE_PAIRING,context.model().getAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE));
      
   }
   @Test
   public void testOnStartAllBridgeAndHubUnPairing() {
      bridgeDevice1.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_UNPAIRING);
      bridgeDevice2.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_UNPAIRING);
      hub.setAttribute(HubCapability.ATTR_STATE, HubCapability.STATE_UNPAIRING);
      start();
      assertEquals(PlaceMonitorSubsystemCapability.PAIRINGSTATE_UNPAIRING,context.model().getAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE));
   }   
   @Test
   public void testOnStartAllBridgeAndHubPartial() {
      bridgeDevice1.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_UNPAIRING);
      bridgeDevice2.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_UNPAIRING);
      hub.setAttribute(HubCapability.ATTR_STATE, HubCapability.STATE_PAIRING);
      start();
      assertEquals(PlaceMonitorSubsystemCapability.PAIRINGSTATE_PARTIAL,context.model().getAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE));
   }   
   @Test
   public void testEventChangePairing() {
      testEventChange(BridgeCapability.PAIRINGSTATE_PAIRING, HubCapability.STATE_PAIRING, PlaceMonitorSubsystemCapability.PAIRINGSTATE_PAIRING);
   }
   @Test
   public void testEventChangeUnPairing() {
      testEventChange(BridgeCapability.PAIRINGSTATE_UNPAIRING, HubCapability.STATE_UNPAIRING, PlaceMonitorSubsystemCapability.PAIRINGSTATE_UNPAIRING);
   }

   @Test
   public void testEventChangeMixed() {
      testEventChange(BridgeCapability.PAIRINGSTATE_UNPAIRING, HubCapability.STATE_NORMAL, PlaceMonitorSubsystemCapability.PAIRINGSTATE_PARTIAL);
   }
   
   public void testEventChange(String bridgeState,String hubState,String finalState) {
      bridgeDevice1.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_IDLE);
      bridgeDevice2.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE, BridgeCapability.PAIRINGSTATE_IDLE);
      hub.setAttribute(HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL);
      start();
      handler.onBridgePairingMode(bridgeDevice1, context);
      assertEquals(PlaceMonitorSubsystemCapability.PAIRINGSTATE_IDLE,context.model().getAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE));

      
      bridgeDevice1.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE,bridgeState);
      store.updateModel(bridgeDevice1.getAddress(), bridgeDevice1.toMap());
      handler.onBridgePairingMode(bridgeDevice1, context);
      assertEquals(PlaceMonitorSubsystemCapability.PAIRINGSTATE_PARTIAL,context.model().getAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE));
      
      bridgeDevice2.setAttribute(BridgeCapability.ATTR_PAIRINGSTATE,bridgeState);
      store.updateModel(bridgeDevice2.getAddress(), bridgeDevice2.toMap());
      handler.onBridgePairingMode(bridgeDevice2, context);
      assertEquals(PlaceMonitorSubsystemCapability.PAIRINGSTATE_PARTIAL,context.model().getAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE));

      hub.setAttribute(HubCapability.ATTR_STATE, hubState);
      store.updateModel(hub.getAddress(), hub.toMap());
      handler.onHubPairingMode(hub, context);
      assertEquals(finalState,context.model().getAttribute(PlaceMonitorSubsystemCapability.ATTR_PAIRINGSTATE));
      

      
   }   

}

