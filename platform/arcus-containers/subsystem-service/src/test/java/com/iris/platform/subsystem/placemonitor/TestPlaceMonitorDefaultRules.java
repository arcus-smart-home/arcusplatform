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
import com.iris.core.template.TemplateService;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceOtaCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.capability.RuleTemplateCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.platform.subsystem.placemonitor.defaultrules.DefaultRuleHandler;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertHandler;

public class TestPlaceMonitorDefaultRules extends SubsystemTestCase<PlaceMonitorSubsystemModel> {
   private PlaceMonitorSubsystem subsystem = null;
   
   Model defaultRuleDevice = new SimpleModel(ModelFixtures.buildDeviceAttributes(DeviceOtaCapability.NAMESPACE).create());

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
   
   @Before
   public void init() throws Exception {
      // can't @Inject this because it uses property injection on a member variable which is not
      // not compatible with @Provides from IrisTestCase for reasons unknown to me
      DefaultRuleHandler handler = new DefaultRuleHandler("classpath:/conf/product-default-rules.xml");
      handler.init();
      
      subsystem = new PlaceMonitorSubsystem(
         ImmutableMap.<String, PlaceMonitorHandler>of(DefaultRuleHandler.class.getName(), handler),
         new PlaceMonitorNotifications(null),
         templateService
      );
      init(subsystem);
   }

   @Test
   public void testOnDefaultRulesDeviceAdded() {
      DeviceModel.setProductId(defaultRuleDevice, "4fccbb");
      store.addModel(defaultRuleDevice.toMap());
      
      assertTrue("should contain device", model.getDefaultRulesDevices().contains(defaultRuleDevice.getAttribute(Capability.ATTR_ADDRESS)));
      MessageBody message = requests.getValue();
      assertEquals("should send ota request", RuleTemplateCapability.CreateRuleRequest.NAME,message.getMessageType());
      assertEquals("Arm On with the Smart Fob", RuleTemplateCapability.CreateRuleRequest.getName(message));
      assertEquals("circle", RuleTemplateCapability.CreateRuleRequest.getContext(message).get("button"));
      assertEquals(defaultRuleDevice.getAddress().getRepresentation(), RuleTemplateCapability.CreateRuleRequest.getContext(message).get("smart fob"));

      assertEquals(placeId.toString(), RuleTemplateCapability.CreateRuleRequest.getPlaceId(message));

   }
}

