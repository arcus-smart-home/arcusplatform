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
package com.iris.platform.rule.catalog.action;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.action.stateful.SendAction;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.platform.rule.catalog.action.config.ParameterConfig;
import com.iris.platform.rule.catalog.action.config.ParameterConfig.ParameterType;
import com.iris.platform.rule.catalog.action.config.SendNotificationActionConfig;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
@Ignore
public class TestNotificationMessageTemplate extends BaseActionTest {
   UUID placeId = UUID.randomUUID();
   UUID personId = UUID.randomUUID();
   
   SimpleContext context = new SimpleContext(
         placeId,
         Address.platformService(UUID.randomUUID(), "rule"), 
         LoggerFactory.getLogger(TestNotificationMessageTemplate.class)
   );
   
   @Test
   public void testStaticValues() {
      NotificationCustomMessageTemplate template = new NotificationCustomMessageTemplate(Collections.emptySet());
      template.setMessage(TemplatedValue.value("The message"));
      template.setTo(TemplatedValue.value(Address.platformService(personId, PersonCapability.NAMESPACE).getRepresentation()));
      template.setMethod(TemplatedValue.value(NotificationCapability.NotifyCustomRequest.DISPATCHMETHOD_EMAIL));
      
      SendAction action = template.generateActionConfig(ImmutableMap.of(RuleTemplate.PLACE_ID, placeId)).createAction(ImmutableMap.of());
      assertEquals(Address.fromString("SERV:note:"), action.getTo(context));
      assertEquals(NotificationCapability.NotifyCustomRequest.NAME, action.getType());
      Map<String, Object> attributes = action.getAttributes(context);
      assertEquals("The message", attributes.get(NotificationCapability.NotifyCustomRequest.ATTR_MSG));
      assertEquals(NotificationCapability.NotifyCustomRequest.DISPATCHMETHOD_EMAIL, attributes.get(NotificationCapability.NotifyCustomRequest.ATTR_DISPATCHMETHOD));
      assertEquals(placeId.toString(), attributes.get(NotificationCapability.NotifyCustomRequest.ATTR_PLACEID));
      assertEquals(personId.toString(), attributes.get(NotificationCapability.NotifyCustomRequest.ATTR_PERSONID));
   }

   @Test
   public void testTemplatedValues() {
      NotificationCustomMessageTemplate template = new NotificationCustomMessageTemplate(Collections.emptySet());
      template.setMessage(TemplatedValue.text("${first} a ${second}"));
      template.setTo(TemplatedValue.named("address", String.class));
      template.setMethod(TemplatedValue.named("method", String.class));

      Map<String, Object> variables =
            ImmutableMap
               .of(
                     "first", "Send",
                     "second", "message",
                     "address", Address.platformService(personId, PersonCapability.NAMESPACE).getRepresentation(),
                     "method", NotificationCapability.NotifyCustomRequest.DISPATCHMETHOD_EMAIL,
                     RuleTemplate.PLACE_ID, placeId
               );
      
      SendAction action = template.generateActionConfig(variables).createAction(ImmutableMap.of());
      assertEquals(Address.fromString("SERV:note:"), action.getTo(context));
      assertEquals(NotificationCapability.NotifyCustomRequest.NAME, action.getType());
      Map<String, Object> attributes = action.getAttributes(context);
      assertEquals("Send a message", attributes.get(NotificationCapability.NotifyCustomRequest.ATTR_MSG));
      assertEquals(NotificationCapability.NotifyCustomRequest.DISPATCHMETHOD_EMAIL, attributes.get(NotificationCapability.NotifyCustomRequest.ATTR_DISPATCHMETHOD));
      assertEquals(placeId.toString(), attributes.get(NotificationCapability.NotifyCustomRequest.ATTR_PLACEID));
      assertEquals(personId.toString(), attributes.get(NotificationCapability.NotifyCustomRequest.ATTR_PERSONID));
   }
   @Test
   public void testNotificationConfigSerialization() throws Exception{
      NotificationMessageTemplate template = new NotificationMessageTemplate(Collections.emptySet());
      template.setParameterConfigs(
            ImmutableList.of(
                  createGetAttributeParameterConfig("device", "${device}", "dev:name"),
                  createConstantParameterConfig("duration", "${duration}")));
            
      template.setKey("some.template.key");
      template.setPriority(TemplatedValue.named("priority", String.class));
      template.setTo(TemplatedValue.named("address", String.class));
      SendNotificationActionConfig config = template.generateActionConfig(testNotifcationVariables());
      SendAction action = (SendAction)serializeDeserialize(config).createAction(ImmutableMap.of());
      action.getTo(context);
   }
   
   private Map<String, Object> testNotifcationVariables(){
      Map<String, Object> variables =
            ImmutableMap.<String,Object>builder()
               .put("first", "Send")
               .put("second", "message")
               .put("address", Address.platformService(personId, PersonCapability.NAMESPACE).getRepresentation())
               .put("priority", "low")
               .put("duration","30")
               .put(RuleTemplate.PLACE_ID, placeId)
               .build();
               
      return variables;
   }
   public ParameterConfig createGetAttributeParameterConfig(String name, String address, String attributeName){
      ParameterConfig config = new ParameterConfig();
      config.setType(ParameterType.ATTRIBUTEVALUE);
      config.setName(name);
      config.setAttributeName(attributeName);
      return config;
   }
   public ParameterConfig createConstantParameterConfig(String name, String value){
      ParameterConfig config = new ParameterConfig();
      config.setType(ParameterType.CONSTANT);
      config.setName(name);
      config.setValue(value);
      return config;
   }


}

