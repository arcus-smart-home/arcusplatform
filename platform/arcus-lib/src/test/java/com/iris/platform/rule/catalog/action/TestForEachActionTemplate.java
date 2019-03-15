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
package com.iris.platform.rule.catalog.action;

import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.rule.action.stateful.ForEachModelAction;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.SimpleModel;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.action.config.ForEachModelActionConfig;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.platform.rule.catalog.template.TemplatedValue;


public class TestForEachActionTemplate extends BaseActionTest {

   private ForEachModelTemplate template;
   private String modelQuery="base:address == '${switch}'";
   LogTemplate logTemplate;
   SimpleContext context;
   Address source = Address.platformService(UUID.randomUUID(), "rule");
   Address modelAddress = Address.platformDriverAddress(UUID.randomUUID());
   
   @Before
   public void init(){
      this.context = new SimpleContext(UUID.randomUUID(), this.source, LoggerFactory.getLogger(TestForEachActionTemplate.class));
      
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_ID, modelAddress.toString());
      model.setAttribute(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE);
      model.setAttribute(Capability.ATTR_ADDRESS, modelAddress.getRepresentation());
      context.putModel(model);

      logTemplate = new LogTemplate();
      logTemplate.setMessage(TemplatedValue.text("hello ${value}"));
      template = new ForEachModelTemplate(ImmutableSet.<String>of("address"));
      template.setActions(ImmutableList.of(logTemplate,logTemplate));
      template.setQuery(new TemplatedExpression(modelQuery));
      template.setVar("address");
   }
   
   @Test
   public void testCreateConfig(){
      ForEachModelActionConfig config = template.generateActionConfig(getVariables());
      String query = TemplatedValue.text(modelQuery).apply(ImmutableMap.of("switch",modelAddress));
      assertEquals(query,config.getModelQuery());
      assertEquals(2,config.getActions().size());
      assertEquals("address", config.getTargetVariable());
   }
   
   @Test
   public void testCreateConfigSerialize() throws Exception{
      ForEachModelActionConfig config = template.generateActionConfig(getVariables());
      ActionConfig config2 = serializeDeserialize(config);
      assertEquals(config,config2);
      ForEachModelAction action = (ForEachModelAction)config2.createAction(ImmutableMap.of());
      action.execute(context);
   }
   private Map<String,Object>getVariables(){
      return ImmutableMap.<String, Object>of("value","world","switch",modelAddress.getRepresentation());
   }
   
 }

