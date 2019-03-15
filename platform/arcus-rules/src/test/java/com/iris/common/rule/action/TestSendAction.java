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
package com.iris.common.rule.action;

import java.util.Collections;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

/**
 * 
 */
public class TestSendAction extends Assert {

   SimpleContext context;
   Address source;
   Address destination;
   Address templateDestination;

   @Before
   public void setUp() throws Exception {
      this.source = Address.platformService(UUID.randomUUID(), "rule");
      this.destination = Address.platformDriverAddress(UUID.randomUUID());
      this.templateDestination = Address.platformDriverAddress(UUID.randomUUID());
      this.context = new SimpleContext(UUID.randomUUID(), this.source, LoggerFactory.getLogger(TestSendAction.class));
   }

   @Test
   public void testSendActionWithNoAttributes() throws Exception {
      SendAction action =
            Actions 
               .buildSendAction("test:Action")
               .withDestination(destination)
               .withAttributes(Collections.<String, Object>emptyMap())
               .build();
      
      assertEquals("send", action.getName());
      assertEquals("send test:Action to " + destination.getRepresentation(), action.getDescription());
      action.execute(context);
      
      {
         PlatformMessage message = context.getMessages().poll();
         assertEquals(source, message.getSource());
         assertEquals(destination, message.getDestination());
         assertEquals("test:Action", message.getMessageType());
         assertEquals("test:Action", message.getValue().getMessageType());
         assertEquals(Collections.emptyMap(), message.getValue().getAttributes());
      }
      
      assertNull(context.getMessages().poll());
   }

   @Test
   public void testSendActionWithAttributes() throws Exception {
      SendAction action =
            Actions 
               .buildSendAction("test:Action")
               .withDestination(destination)
               .withAttribute("test:value", "test")
               .build();
      
      assertEquals("send", action.getName());
      assertEquals("send test:Action({test:value=test}) to " + destination.getRepresentation(), action.getDescription());
      action.execute(context);
      {
         PlatformMessage message = context.getMessages().poll();
         assertEquals(source, message.getSource());
         assertEquals(destination, message.getDestination());
         assertEquals("test:Action", message.getMessageType());
         assertEquals("test:Action", message.getValue().getMessageType());
         assertTrue(message.isRequest());
         assertFalse(message.isError());
         assertNull(message.getCorrelationId());
         assertEquals(Collections.singletonMap("test:value", "test"), message.getValue().getAttributes());
      }
      
      assertNull(context.getMessages().poll());
   }

   @Test
   public void testSendActionWithTemplatedDestination() throws Exception {
      SendAction action =
            Actions 
               .buildSendAction("test:Action")
               .withTemplatedDestination()
               .withAttribute("test:value", "test")
               .build();
      
      context.setVariable(SendAction.VAR_TO, templateDestination);
      context.setVariable(SendAction.VAR_ATTRIBUTES, Collections.singletonMap("test:TemplateVar", "templated"));
      
      assertEquals("send", action.getName());
      assertEquals("send test:Action({test:value=test}) to ${to}", action.getDescription());
      action.execute(context);
      {
         PlatformMessage message = context.getMessages().poll();
         assertEquals(source, message.getSource());
         assertEquals(templateDestination, message.getDestination());
         assertEquals("test:Action", message.getMessageType());
         assertEquals("test:Action", message.getValue().getMessageType());
         assertTrue(message.isRequest());
         assertFalse(message.isError());
         assertNull(message.getCorrelationId());
         assertEquals(Collections.singletonMap("test:value", "test"), message.getValue().getAttributes());
      }
      
      assertNull(context.getMessages().poll());
   }

   @Test
   public void testSendActionWithTemplatedDestinationUnspecified() throws Exception {
      SendAction action =
            Actions 
               .buildSendAction("test:Action")
               .withTemplatedDestination()
               .withAttribute("test:value", "test")
               .build();
      
      assertEquals("send", action.getName());
      assertEquals("send test:Action({test:value=test}) to ${to}", action.getDescription());
      action.execute(context);
      
      // this is an error, no message can be sent
      
      assertNull(context.getMessages().poll());
   }

   @Test
   public void testSendActionWithTemplatedAttributes() throws Exception {
      SendAction action =
            Actions 
               .buildSendAction("test:Action")
               .withDestination(destination)
               .withTemplatedAttributes()
               .build();
      
      context.setVariable(SendAction.VAR_TO, templateDestination);
      context.setVariable(SendAction.VAR_ATTRIBUTES, Collections.singletonMap("test:TemplateVar", "templated"));
      
      assertEquals("send", action.getName());
      assertEquals("send test:Action(${attributes}) to " + destination.getRepresentation(), action.getDescription());
      action.execute(context);
      
      {
         PlatformMessage message = context.getMessages().poll();
         assertEquals(source, message.getSource());
         assertEquals(destination, message.getDestination());
         assertEquals("test:Action", message.getMessageType());
         assertEquals("test:Action", message.getValue().getMessageType());
         assertTrue(message.isRequest());
         assertFalse(message.isError());
         assertNull(message.getCorrelationId());
         assertEquals(Collections.singletonMap("test:TemplateVar", "templated"), message.getValue().getAttributes());
      }
      
      assertNull(context.getMessages().poll());
   }

   @Test
   public void testSendActionWithTemplatedAttributesUnspecified() throws Exception {
      SendAction action =
            Actions 
               .buildSendAction("test:Action")
               .withDestination(destination)
               .withTemplatedAttributes()
               .build();
      
      assertEquals("send", action.getName());
      assertEquals("send test:Action(${attributes}) to " + destination.getRepresentation(), action.getDescription());
      action.execute(context);
      
      {
         PlatformMessage message = context.getMessages().poll();
         assertEquals(source, message.getSource());
         assertEquals(destination, message.getDestination());
         assertEquals("test:Action", message.getMessageType());
         assertEquals("test:Action", message.getValue().getMessageType());
         assertTrue(message.isRequest());
         assertFalse(message.isError());
         assertNull(message.getCorrelationId());
         assertEquals(Collections.emptyMap(), message.getValue().getAttributes());
      }
      
      assertNull(context.getMessages().poll());
   }
}

