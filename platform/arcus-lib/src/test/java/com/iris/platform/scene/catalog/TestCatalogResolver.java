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

package com.iris.platform.scene.catalog;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import com.iris.messages.MessageBody;
import com.iris.platform.scene.resolver.ThermostatResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.common.rule.action.Action;
import com.iris.io.xml.JAXBUtil;
import com.iris.messages.MessagesModule;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.CameraCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.Somfyv1Capability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.type.ActionSelector;
import com.iris.messages.type.ThermostatAction;
import com.iris.platform.scene.catalog.serializer.ActionTemplateType;
import com.iris.platform.scene.catalog.serializer.SceneCatalog;
import com.iris.platform.scene.resolver.CatalogActionTemplateResolver;
import com.iris.platform.scene.resolver.ShadeResolver;
import com.iris.resource.Resources;
import com.iris.test.Modules;

import static com.iris.Utils.assertTrue;
import static com.iris.messages.MessageBody.buildMessage;

@Modules({ CapabilityRegistryModule.class, MessagesModule.class })
public class TestCatalogResolver extends SceneCatalogBaseTest {
   
   private Map<String,CatalogActionTemplateResolver>resolverMap;
   private CatalogActionTemplateResolver resolver;
   private CatalogActionTemplateResolver fanResolver;
   @Inject CapabilityRegistry registry;
   SceneCatalog sc;
   Model testSwitch;
   Model testFan;
   
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      testSwitch = addDevice(SwitchCapability.NAMESPACE,DimmerCapability.NAMESPACE);
      testFan = addDevice(SwitchCapability.NAMESPACE,FanCapability.NAMESPACE);
      testFan.setAttribute(FanCapability.ATTR_MAXSPEED, 5);
      sc = JAXBUtil.fromXml(Resources.getResource("classpath:/com/iris/conf/scene-catalog.xml"), SceneCatalog.class);
      resolverMap=sc.getActionTemplates().getActionTemplate().stream().collect(
               Collectors.toMap(ActionTemplateType::getTypeHint, (p) -> new CatalogActionTemplateResolver(registry, p)));
      
      resolver = resolverMap.get("lights");
      fanResolver = resolverMap.get("fan");

   }

   @After
   public void tearDown() throws Exception {
   }

   @Test
   public void testDimmerSwitch() {
      List<ActionSelector>actionSelectors=resolver.resolve(context,testSwitch);
      assertEquals(1, actionSelectors.size());
      ActionSelector selector1 = actionSelectors.get(0);
      
      assertEquals("switch", selector1.getName());
      assertEquals(ActionSelector.TYPE_GROUP, selector1.getType().toUpperCase());
      List<List<Object>> values = (List<List<Object>>)selector1.getValue();
      ActionSelector subselector = new ActionSelector(ImmutableMap.of(ActionSelector.ATTR_TYPE,ActionSelector.TYPE_PERCENT,ActionSelector.ATTR_NAME,"dim"));
      List<List<Object>> expecting = ImmutableList.of(ImmutableList.of("ON",ImmutableList.of(subselector.toMap())),ImmutableList.of("OFF",ImmutableList.of()));
      assertEquals(expecting, values);
      
      Action action = resolver.generate(context, testSwitch.getAddress(), ImmutableMap.<String,Object>of("switch", "ON","dim","50"));
      action.execute(context);
      BlockingQueue<PlatformMessage> messages = context.getMessages();
      PlatformMessage message = messages.remove();
      assertEquals("base:SetAttributes", message.getValue().getMessageType());
      // turned to a long via json serialization
      assertEquals(50L, message.getValue().getAttributes().get(DimmerCapability.ATTR_BRIGHTNESS));
      assertEquals("ON", message.getValue().getAttributes().get(SwitchCapability.ATTR_STATE));
   }
   
   @Test
   public void testTurnOnFan() {
      List<ActionSelector>actionSelectors=fanResolver.resolve(context,testFan);
      assertEquals(1, actionSelectors.size());
      ActionSelector selector1 = actionSelectors.get(0);
      
      assertEquals("switch", selector1.getName());
      assertEquals(ActionSelector.TYPE_GROUP, selector1.getType().toUpperCase());
      List<List<Object>> values = (List<List<Object>>)selector1.getValue();
      ActionSelector expedctingSubselector = new ActionSelector(ImmutableMap.of(ActionSelector.ATTR_TYPE,ActionSelector.TYPE_LIST,ActionSelector.ATTR_NAME,"fanspeed"));
      expedctingSubselector.setValue(ImmutableList.of(ImmutableList.of("LOW",1),ImmutableList.of("MEDIUM",3),ImmutableList.of("HIGH",5)));
      List<List<Object>> expecting = ImmutableList.of(ImmutableList.of("ON",ImmutableList.of(expedctingSubselector.toMap())),ImmutableList.of("OFF",ImmutableList.of()));
      assertEquals(expecting, values);
      
      Action action = fanResolver.generate(context, testFan.getAddress(), ImmutableMap.<String,Object>of("switch", "ON","fanspeed","1"));
      action.execute(context);
      BlockingQueue<PlatformMessage> messages = context.getMessages();
      PlatformMessage message = messages.remove();
      assertEquals("base:SetAttributes", message.getValue().getMessageType());
      assertEquals("ON", message.getValue().getAttributes().get(SwitchCapability.ATTR_STATE));
      assertEquals(1L, message.getValue().getAttributes().get(FanCapability.ATTR_SPEED));
   }
   
   @Test
   public void testTurnOffFan() {
      List<ActionSelector>actionSelectors=fanResolver.resolve(context,testFan);
      assertEquals(1, actionSelectors.size());
      ActionSelector selector1 = actionSelectors.get(0);
      
      assertEquals("switch", selector1.getName());
      assertEquals(ActionSelector.TYPE_GROUP, selector1.getType().toUpperCase());
      List<List<Object>> values = (List<List<Object>>)selector1.getValue();
      ActionSelector expectedSubselector = new ActionSelector(ImmutableMap.of(ActionSelector.ATTR_TYPE,ActionSelector.TYPE_LIST,ActionSelector.ATTR_NAME,"fanspeed"));
      expectedSubselector.setValue(ImmutableList.of(ImmutableList.of("LOW",1),ImmutableList.of("MEDIUM",3),ImmutableList.of("HIGH",5)));
      List<List<Object>> expecting = ImmutableList.of(ImmutableList.of("ON",ImmutableList.of(expectedSubselector.toMap())),ImmutableList.of("OFF",ImmutableList.of()));
      assertEquals(expecting, values);
      
      Action action = fanResolver.generate(context, testFan.getAddress(), ImmutableMap.<String,Object>of("switch", "OFF"));
      action.execute(context);
      BlockingQueue<PlatformMessage> messages = context.getMessages();
      PlatformMessage message = messages.remove();
      assertEquals("base:SetAttributes", message.getValue().getMessageType());
      assertEquals("OFF", message.getValue().getAttributes().get(SwitchCapability.ATTR_STATE));
      assertEquals(null, message.getValue().getAttributes().get(FanCapability.ATTR_SPEED));
   }

   @Test
   public void testFanmodeOn() {

      ThermostatResolver test = new ThermostatResolver();
      Model testThermostat = createThermostat(1.67, 35.0, 1.67);

      ThermostatAction taction = createThermostatAction(30.0, 15.0, ThermostatAction.MODE_AUTO, false);
      taction.setFanmode(1);
      Map<String,Object>variables=ImmutableMap.of("thermostat",taction.toMap());

      Action holder = test.generate(context, testThermostat.getAddress(), variables);
      holder.execute(context);
      BlockingQueue<PlatformMessage> messages = context.getMessages();
      PlatformMessage message = messages.remove();
      assertEquals("subclimate:DisableScheduler", message.getValue().getMessageType());
      message = messages.remove();
      assertTrue(Long.valueOf("1") == message.getValue().getAttributes().get("therm:fanmode"));
   }

   @Test
   public void testThermostatResolver() {
      resolver=resolverMap.get("thermostat");
      Model testThermostat = createThermostat(1.67, 35.0, 1.67);         
      ThermostatAction taction = createThermostatAction(30.0, 15.0, ThermostatAction.MODE_AUTO, false);

      doExecuteThermostatActionOK(taction, testThermostat);
      
   }
   
   //Preconditions.checkArgument(Precision.compareTo(action.getHeatSetPoint(), minSetPoint, PRECISION) > 0 , "The heatsetpoint can't be set below minSetPoint");
   @Test
   public void testThermostatResolver_OK_1() {
      resolver=resolverMap.get("thermostat");
      Model testThermostat = createThermostat(1.67, 35.0, 1.67);   
      //HeatSetPoint 1.661 is lower than min minSetPoint 1.67, but still within 0.01 tolerance
      ThermostatAction taction = createThermostatAction(30.0, 1.661, ThermostatAction.MODE_AUTO, false);
      
      doExecuteThermostatActionOK(taction, testThermostat);
   }
   
   @Test
   public void testThermostatResolver_Error_1() {
      resolver=resolverMap.get("thermostat");
      Model testThermostat = createThermostat(1.67, 35.0, 1.67);   
      //HeatSetPoint 1.655 is lower than minSetPoint 1.67, and not within 0.01 tolerance
      ThermostatAction taction = createThermostatAction(30.0, 1.655, ThermostatAction.MODE_AUTO, false);
      
      try{
         doExecuteThermostatActionOK(taction, testThermostat);
         fail("should have failed");
      }catch(Exception e) {
         //ok
      }
   }
   
   @Test
   public void testThermostatResolver_OK_2() {
      resolver=resolverMap.get("thermostat");
      Model testThermostat = createThermostat(1.67, 35.0, 1.67);   
      //HeatSetPoint 1.67 is same as minSetPoint 1.67, ok
      ThermostatAction taction = createThermostatAction(30.0, 1.67, ThermostatAction.MODE_AUTO, false);
      
      doExecuteThermostatActionOK(taction, testThermostat);
   }
   
   private void doExecuteThermostatActionOK(ThermostatAction taction, Model testThermostat) {
      Map<String,Object>variables=ImmutableMap.of("thermostat",taction.toMap());
      Action action = resolver.generate(context, testThermostat.getAddress(),variables);
      action.execute(context);
      BlockingQueue<PlatformMessage> messages = context.getMessages();
      PlatformMessage message = messages.remove();
      assertNotNull(message);
   }
   
   private Model createThermostat(Double minSetPoint, Double maxSetPoint, Double setPointSeperation) {
      return addDevice(ImmutableMap.<String, Object>of(
         ThermostatCapability.ATTR_MINSETPOINT, minSetPoint,
         ThermostatCapability.ATTR_MAXSETPOINT, maxSetPoint,
         ThermostatCapability.ATTR_SETPOINTSEPARATION, setPointSeperation),          
         ThermostatCapability.NAMESPACE);
   }
   
   private ThermostatAction createThermostatAction(Double coolSetPoint, Double heatSetPoint, String mode, boolean scheduleEnabled) {
      ThermostatAction taction = new ThermostatAction();
      taction.setCoolSetPoint(coolSetPoint);
      taction.setHeatSetPoint(heatSetPoint);  
      taction.setMode(mode);
      taction.setScheduleEnabled(scheduleEnabled);
      return taction;
   }
   
   @Test
   public void testSecurityResolver() {
      resolver=resolverMap.get("security");
      Model testSecurity = addDevice(SecuritySubsystemCapability.NAMESPACE);

      List<ActionSelector>selectors = resolver.resolve(context, testSecurity);
      ActionSelector selector1 = selectors.get(0);
      List<List<Object>> values = (List<List<Object>>)selector1.getValue();
      assertEquals("alarm-state", selector1.getName());
      assertEquals(ActionSelector.TYPE_LIST, selector1.getType().toUpperCase());
      assertEquals("Arm On", values.get(0).get(0));
      assertEquals("ON", values.get(0).get(1));
      assertEquals("Arm Partial", values.get(1).get(0));
      assertEquals("PARTIAL", values.get(1).get(1));
      assertEquals("Disarm", values.get(2).get(0));
      assertEquals("OFF", values.get(2).get(1));
      
      
      
      Map<String,Object>variables=ImmutableMap.of("alarm-state","ON");
      Action action = resolver.generate(context, testSecurity.getAddress(),variables);
      action.execute(context);
      BlockingQueue<PlatformMessage> messages = context.getMessages();
      PlatformMessage message = messages.remove();
      assertEquals("subsecurity:ArmBypassed",message.getMessageType());
   }
   
   @Test
   public void testCameraResolver() {
      resolver=resolverMap.get("camera");
      Model testSecurity = addDevice(CameraCapability.NAMESPACE);
      Map<String,Object>variables=ImmutableMap.of("duration","30");
      Action action = resolver.generate(context, testSecurity.getAddress(),variables);
      action.execute(context);
      BlockingQueue<PlatformMessage> messages = context.getMessages();
      PlatformMessage message = messages.remove();
      assertEquals("video:StartRecording",message.getMessageType());
   }
   
   @Test
   public void testSomfyBlindsResolver() {
      resolver=resolverMap.get("blind");
      Model testBlinds = addDevice(Somfyv1Capability.NAMESPACE);
      Map<String,Object>variables=ImmutableMap.of(ShadeResolver.SOMFY_SELECTOR_NAME,Somfyv1Capability.CURRENTSTATE_OPEN);
      Action action = resolver.generate(context, testBlinds.getAddress(),variables);
      action.execute(context);
      BlockingQueue<PlatformMessage> messages = context.getMessages();
      PlatformMessage message = messages.remove();
      assertEquals("somfyv1:GoToOpen",message.getMessageType());
   }
   

}

