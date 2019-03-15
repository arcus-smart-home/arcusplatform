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
package com.iris.platform.rule.bugz;

import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.iris.common.rule.Rule;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.common.rule.simple.SimpleRule;
import com.iris.io.json.JSON;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ButtonCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.ChildId;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.rule.RuleEnvironment;
import com.iris.platform.rule.StatefulRuleDefinition;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;
import com.iris.util.TypeMarker;

@Modules({ RuleConfigJsonModule.class })
public class TestRuleBug extends IrisTestCase {
   private static final Logger logger = LoggerFactory.getLogger(TestRuleBug.class);

   UUID placeId = UUID.fromString("b7a602bf-c387-4fec-922f-08ceab2b24e0");
   Address ruleAddress = Address.platformService(placeId, "rule", 53);
   Address buttonAddress = Address.fromString("DRIV:dev:e5d638b1-55dc-4183-8140-6cbae42affc4");
   Address switchAddress = Address.fromString("DRIV:dev:1eb05242-f689-4d36-a08f-5f5db128f8c6");
   
   public TestRuleBug() {
      // TODO Auto-generated constructor stub
   }

   @Test
   public void testToggleOff() {
      StatefulRuleDefinition definition = createDefinition();
      RuleEnvironment environment = new RuleEnvironment();
      environment.setPlaceId(placeId);
      environment.setActions(ImmutableList.of());
      environment.setScenes(ImmutableList.of());
      environment.setRules(ImmutableList.of(definition));
      SimpleContext context = createContext();
      context.getModelByAddress(switchAddress).setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      
      Rule rule = generateRule(definition, context, environment, ruleAddress);
      rule.activate();
      context.getModelByAddress(buttonAddress).setAttribute(ButtonCapability.ATTR_STATE, ButtonCapability.STATE_PRESSED);
      rule.execute(AttributeValueChangedEvent.create(buttonAddress, ButtonCapability.ATTR_STATE, ButtonCapability.STATE_PRESSED, ButtonCapability.STATE_RELEASED));
      
      PlatformMessage toggleSwitch = context.getMessages().poll();
      assertNotNull(toggleSwitch);
      assertEquals(switchAddress, toggleSwitch.getDestination());
      assertEquals(SwitchCapability.STATE_OFF, toggleSwitch.getValue().getAttributes().get(SwitchCapability.ATTR_STATE));
   }
   
   @Test
   public void testToggleOn() {
      StatefulRuleDefinition definition = createDefinition();
      RuleEnvironment environment = new RuleEnvironment();
      environment.setPlaceId(placeId);
      environment.setActions(ImmutableList.of());
      environment.setScenes(ImmutableList.of());
      environment.setRules(ImmutableList.of(definition));
      SimpleContext context = createContext();
      context.getModelByAddress(switchAddress).setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      
      Rule rule = generateRule(definition, context, environment, ruleAddress);
      rule.activate();
      context.getModelByAddress(buttonAddress).setAttribute(ButtonCapability.ATTR_STATE, ButtonCapability.STATE_PRESSED);
      rule.execute(AttributeValueChangedEvent.create(buttonAddress, ButtonCapability.ATTR_STATE, ButtonCapability.STATE_PRESSED, ButtonCapability.STATE_RELEASED));
      
      PlatformMessage toggleSwitch = context.getMessages().poll();
      assertNotNull(toggleSwitch);
      assertEquals(switchAddress, toggleSwitch.getDestination());
      assertEquals(SwitchCapability.STATE_ON, toggleSwitch.getValue().getAttributes().get(SwitchCapability.ATTR_STATE));
   }
   
   public SimpleContext createContext() {
      SimpleContext context = new SimpleContext(placeId, ruleAddress, logger);
      context.putModel(
            ModelFixtures
               .buildDeviceAttributes((UUID) buttonAddress.getId(), ButtonCapability.NAMESPACE)
               .put(ButtonCapability.ATTR_STATE, ButtonCapability.STATE_RELEASED)
               .toModel()
      );
      context.putModel(
            ModelFixtures
               .buildDeviceAttributes((UUID) switchAddress.getId(), SwitchCapability.NAMESPACE)
               .put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF)
               .toModel()
      );
      return context;
   }

   public StatefulRuleDefinition createDefinition() {
      StatefulRuleDefinition definition = new StatefulRuleDefinition();
      definition.setId(new ChildId(placeId, 53));
      definition.setAction(JSON.fromJson(
            "{\"type\":\"actions\",\"actionConfigs\":[{\"type\":\"for-each-model\",\"actions\":[{\"type\":\"set-attribute\",\"address\":\"${address}\",\"attributeName\":\"swit:state\",\"attributeValue\":\"OFF\",\"attributeType\":\"enum\u003cON,OFF\u003e\",\"duration\":0,\"unit\":\"SECONDS\",\"conditionQuery\":null,\"reevaluateCondition\":false}],\"modelQuery\":\"base:address \u003d\u003d \u0027DRIV:dev:1eb05242-f689-4d36-a08f-5f5db128f8c6\u0027 and swit:state \u003d\u003d \u0027ON\u0027\",\"targetVariable\":\"address\"},{\"type\":\"for-each-model\",\"actions\":[{\"type\":\"set-attribute\",\"address\":\"${address}\",\"attributeName\":\"swit:state\",\"attributeValue\":\"ON\",\"attributeType\":\"enum\u003cON,OFF\u003e\",\"duration\":0,\"unit\":\"SECONDS\",\"conditionQuery\":null,\"reevaluateCondition\":false}],\"modelQuery\":\"base:address \u003d\u003d \u0027DRIV:dev:1eb05242-f689-4d36-a08f-5f5db128f8c6\u0027 and swit:state !\u003d \u0027ON\u0027\",\"targetVariable\":\"address\"}]}", 
            ActionConfig.class
      ));
      definition.setCondition(JSON.fromJson(
            "{\"type\":\"value-change\",\"attribute\":\"but:state\",\"oldValue\":null,\"newValue\":\"PRESSED\",\"query\":\"base:address \u003d\u003d \u0027${button}\u0027\"}",
            ConditionConfig.class
      ));
      definition.setRuleTemplate("16536e");
      definition.setVariables(JSON.fromJson(
            "{\"_ruleName\":\"Control Devices with a Button\",\"newValue:0:1eb05242-f689-4d36-a08f-5f5db128f8c6:0\":\"OFF\",\"_stillFiring:0\":[],\"_stillFiring:1\":[],\"_firing\":false,\"attribute:0:1eb05242-f689-4d36-a08f-5f5db128f8c6:0\":\"swit:state\",\"oldValue:0:1eb05242-f689-4d36-a08f-5f5db128f8c6:0\":\"ON\",\"switch\":\"DRIV:dev:1eb05242-f689-4d36-a08f-5f5db128f8c6\",\"button\":\"DRIV:dev:e5d638b1-55dc-4183-8140-6cbae42affc4\",\"_accountId\":\"b0c377c6-73a5-4f23-b8ab-c5d4de4961e9\",\"to:0:1eb05242-f689-4d36-a08f-5f5db128f8c6:0\":\"DRIV:dev:1eb05242-f689-4d36-a08f-5f5db128f8c6\",\"_placeId\":\"b7a602bf-c387-4fec-922f-08ceab2b24e0\",\"address:1eb05242-f689-4d36-a08f-5f5db128f8c6:1\":\"DRIV:dev:1eb05242-f689-4d36-a08f-5f5db128f8c6\",\"address:1eb05242-f689-4d36-a08f-5f5db128f8c6:0\":\"DRIV:dev:1eb05242-f689-4d36-a08f-5f5db128f8c6\"}",
            new TypeMarker<Map<String, Object>>() {}
      ));
      return definition;
   }
   
   // copied from DefaultPlaceExecutorFactory
   private Rule generateRule(RuleDefinition definition, RuleContext context, RuleEnvironment environment, Address ruleAddress) {
      Condition condition = definition.createCondition(environment);
      StatefulAction action = definition.createAction(environment);
      
      return new SimpleRule(context, condition, action, ruleAddress);
   }

}

