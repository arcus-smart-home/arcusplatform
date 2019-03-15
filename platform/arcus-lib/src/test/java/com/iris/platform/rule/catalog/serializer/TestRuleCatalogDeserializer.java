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
/**
 *
 */
package com.iris.platform.rule.catalog.serializer;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.stateful.LogAction;
import com.iris.common.rule.action.stateful.SendAction;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.io.Deserializer;
import com.iris.messages.address.Address;
import com.iris.messages.type.Population;
import com.iris.platform.rule.catalog.ActionTemplate;
import com.iris.platform.rule.catalog.RuleCatalog;
import com.iris.platform.rule.catalog.RuleCatalogMetadata;
import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.platform.rule.catalog.action.LogTemplate;
import com.iris.platform.rule.catalog.action.NotificationCustomMessageTemplate;
import com.iris.platform.rule.catalog.action.SendMessageTemplate;
import com.iris.platform.rule.catalog.action.SetAndRestoreTemplate;
import com.iris.platform.rule.catalog.condition.config.DayOfWeekConfig;
import com.iris.platform.rule.catalog.condition.config.DurationConfig;
import com.iris.platform.rule.catalog.condition.config.FilterConfig;
import com.iris.platform.rule.catalog.condition.config.ValueChangeConfig;
import com.iris.platform.rule.catalog.selector.SelectorGenerator;
import com.iris.resource.Resources;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 *
 */
@Modules( CapabilityRegistryModule.class )
public class TestRuleCatalogDeserializer extends IrisTestCase {

   @Inject
   CapabilityRegistry registry;

   Map<String,RuleCatalog> catalogs;
   ActionContext context;


   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      Deserializer<Map<String,RuleCatalog>> deserializer = new RuleCatalogDeserializer(registry);
      try(InputStream is = Resources.open("classpath:///catalog.xml")) {
         catalogs = deserializer.deserialize(is);
      }
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestRuleCatalogDeserializer.class));
   }

   private RuleCatalog general() {
      return catalogs.get(Population.NAME_GENERAL);
   }

   private RuleCatalog alpha() {
      return catalogs.get("alpha");
   }

   @Test
   public void testMetadata() throws Exception {
      RuleCatalogMetadata metadata = general().getMetadata();
      assertEquals(new Date(1429813389000L), metadata.getVersion());
      assertEquals("6a757c1cd50d9486df652ef579b4e954", metadata.getHash());
      assertEquals("Human", metadata.getPublisher());
      assertEquals(15, general().getCategories().size());
      assertEquals(15, alpha().getCategories().size());
   }

   @Test
   public void testTemplateMetadata() throws Exception {
      {
         RuleTemplate template = general().getById("00000");
         assertNotNull(template);
         assertEquals("00000", template.getId());
         assertEquals(ImmutableSet.of("motion"), template.getKeywords());
         assertEquals(ImmutableSet.of("dev", "mot"), template.getTags());
         assertEquals(ImmutableSet.of(Population.NAME_GENERAL), template.getPopulations());
         assertEquals(new Date(1435708800000L), template.getCreated());
         assertEquals(new Date(1435708800000L), template.getModified());
         assertEquals("When motion detected turn on light", template.getName());
         assertEquals("When motion is detected, Turn on ${device}", template.getTemplate());
         assertEquals(ImmutableSet.of("Cameras & Sensors", "Lights & Switches"), template.getCategories());
         assertEquals("Template" + template.getId() + " description", template.getDescription());
      }
      {
         RuleTemplate template = general().getById("00001");
         assertNotNull(template);
         assertEquals("00001", template.getId());
         assertEquals(ImmutableSet.of("motion", "presence"), template.getKeywords());
         assertEquals(ImmutableSet.of("dev", "mot"), template.getTags());
         assertEquals(ImmutableSet.of(Population.NAME_GENERAL), template.getPopulations());
         assertEquals(new Date(1426682473000L), template.getCreated());
         assertEquals(new Date(1429813389000L), template.getModified());
         assertEquals("When motion not detected notify person", template.getName());
         assertEquals("When motion is not detected by ${sensor-selector} on ${day-of-week} for ${duration}, Notify ${person} by ${method}", template.getTemplate());
         assertEquals(ImmutableSet.of("Cameras & Sensors", "Notifications"), template.getCategories());
         assertEquals("Template" + template.getId() + " description", template.getDescription());
      }
      {
         RuleTemplate template = alpha().getById("00002");
         assertNotNull(template);
         assertEquals("00002", template.getId());
         assertEquals(ImmutableSet.of("contact", "door", "video"), template.getKeywords());
         assertEquals(ImmutableSet.of("dev", "cont", "vid"), template.getTags());
         assertEquals(ImmutableSet.of("alpha", "beta"), template.getPopulations());
         assertEquals(new Date(1426682473000L), template.getCreated());
         assertEquals(new Date(1429813389000L), template.getModified());
         assertEquals("When door is opened record video", template.getName());
         assertEquals("When the ${door} is opened, Record video from ${camera} for ${duration} seconds", template.getTemplate());
         assertEquals(ImmutableSet.of("Cameras & Sensors", "Doors & Locks", "Security"), template.getCategories());
         assertNull(template.getDescription());
      }
   }

   @Test
   public void testTemplatesByCategory() {
      Map<String,Integer> counts = general().getRuleCountByCategory();
      counts.entrySet().forEach((e) -> {
         List<RuleTemplate> templates = general().getTemplatesForCategory(e.getKey());
         assertEquals(e.getValue().intValue(), templates.size());
      });
   }

   @Test
   public void testValueChangeCondition() throws Exception {
      {
         ValueChangeConfig condition = (ValueChangeConfig) general().getById("00000").getCondition();
         assertEquals("mot:motion", condition.getAttributeExpression().getExpression());
         assertEquals(null, condition.getOldValueExpression());
         assertNotNull(condition.getNewValueExpression());
      }
      {
         ValueChangeConfig condition = (ValueChangeConfig) alpha().getById("00002").getCondition();
         assertEquals("cont:contact", condition.getAttributeExpression().getExpression());
         assertNotNull(condition.getOldValueExpression());
         assertNotNull(condition.getNewValueExpression());
      }
   }

   @Test
   public void testDurationCondition() throws Exception {
      DurationConfig condition = (DurationConfig) ((FilterConfig) general().getById("00001").getCondition()).getCondition();
      assertEquals(100, condition.getDurationExpression().toTemplate().apply(ImmutableMap.of("duration", 100)));
      assertNotNull(condition.getMatcherExpression());
      assertNotNull(condition.getSelectorExpression());
   }

   @Test
   public void testDayOfWeekCondition() throws Exception {
      {
         DayOfWeekConfig condition = (DayOfWeekConfig) general().getById("00001").getCondition();
         assertNotNull(condition.getDayExpression());
      }
   }

   @Test
   public void testSetAttributeAction() throws Exception {
      List<ActionTemplate> actions = general().getById("00000").getActions();
      assertEquals(1, actions.size());
      {
         SetAndRestoreTemplate action = (SetAndRestoreTemplate) actions.get(0);
         assertNotNull(action.getAddress());
         assertEquals("swit:state", action.getAttributeName());
         assertNotNull(action.getAttributeValue());
         assertEquals("0", action.getDuration().getExpression());
         assertEquals(TimeUnit.SECONDS, action.getUnit());
      }
   }

   @Test
   public void testSetAttributeWithDurationAction() throws Exception {
      List<ActionTemplate> actions = general().getById("00003").getActions();
      assertEquals(1, actions.size());
      {
         SetAndRestoreTemplate action = (SetAndRestoreTemplate) actions.get(0);
         assertNotNull(action.getAddress());
         assertEquals("swit:state", action.getAttributeName());
         assertNotNull(action.getAttributeValue());
         assertEquals("${duration}", action.getDuration().getExpression());
         assertEquals(TimeUnit.MINUTES, action.getUnit());
      }
   }
   
   @Test
   public void testSetAttributeWithConditionQueryAction() throws Exception {
      List<ActionTemplate> actions = general().getById("test1e1f0d").getActions();
      assertEquals(1, actions.size());
      {
         SetAndRestoreTemplate action = (SetAndRestoreTemplate) actions.get(0);
         assertNotNull(action.getAddress());
         assertEquals("swit:state", action.getAttributeName());
         assertNotNull(action.getAttributeValue());
         assertEquals("${for awhile}", action.getDuration().getExpression());
         assertEquals(TimeUnit.SECONDS, action.getUnit());
         assertEquals("base:address = '${address}' AND mot:motion = 'NONE'", action.getConditionQuery().getExpression());
      }
   }

   @Test
   public void testNotifyAction() throws Exception {
      List<ActionTemplate> actions = general().getById("00001").getActions();
      assertEquals(1, actions.size());
      {
         NotificationCustomMessageTemplate action = (NotificationCustomMessageTemplate) actions.get(0);
         assertNotNull(action.getTo());
         assertNotNull(action.getMessage());
      }
   }

   @Test
   public void testSendAction() throws Exception {
      List<ActionTemplate> actions = alpha().getById("00002").getActions();
      {
         Address deviceAddress = Address.platformDriverAddress(UUID.randomUUID());

         SendMessageTemplate send = (SendMessageTemplate) actions.get(0);
         assertNotNull(send.getTo());
         assertNotNull(send.getType());
         assertNotNull(send.getAttributes());

         // TODO move this into separate test cases
         Map<String, Object> variables =
               ImmutableMap.of(
                  "camera", deviceAddress.getRepresentation(),
                  "duration", 10000
               );
         SendAction action = send.generateActionConfig(variables).createAction(null);
         assertEquals("cam:Record", action.getType());

         assertEquals(deviceAddress, action.getTo(context));
         assertEquals(ImmutableMap.of("duration", "10000"), action.getAttributes(null));
      }
   }

   @Test
   public void testLogAction() throws Exception {
      List<ActionTemplate> actions = alpha().getById("00002").getActions();
      {
         LogTemplate send = (LogTemplate) actions.get(1);
         assertNotNull(send.getMessage());

         LogAction action = send.generateActionConfig(ImmutableMap.of()).createAction(null);
         assertNotNull(action);
      }
   }

   @Test
   public void testSelectors() throws Exception {
      {
         Map<String, SelectorGenerator> selectors = general().getById("00000").getOptions();
         assertEquals(
               ImmutableSet.of("device"),
               selectors.keySet()
         );
      }
      {
         Map<String, SelectorGenerator> selectors = general().getById("00001").getOptions();
         assertEquals(
               ImmutableSet.of("sensor-selector", "day-of-week", "person"),
               selectors.keySet()
         );
      }
      {
         Map<String, SelectorGenerator> selectors = alpha().getById("00002").getOptions();
         assertEquals(
               ImmutableSet.of("door", "camera", "duration"),
               selectors.keySet()
         );
      }
   }
}

