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
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.SimpleModel;

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

   @Test
   public void testSetAttributesSkippedWhenAlreadyInDesiredState() throws Exception {
      // Set up a model at the destination with current attribute values
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TYPE, "dev");
      model.setAttribute(Capability.ATTR_ID, destination.getId().toString());
      model.setAttribute(Capability.ATTR_ADDRESS, destination.getRepresentation());
      model.setAttribute("swit:state", "ON");
      model.setAttribute("dim:brightness", 100);
      context.putModel(model);
      context.setVariable("filterUnchanged", Boolean.TRUE);

      SendAction action =
            Actions
               .buildSendAction(Capability.CMD_SET_ATTRIBUTES)
               .withDestination(destination)
               .withAttribute("swit:state", "ON")
               .withAttribute("dim:brightness", 100)
               .build();

      action.execute(context);

      // No message should be sent since device is already in desired state
      assertNull("Expected no message when device is already in desired state",
            context.getMessages().poll());
   }

   @Test
   public void testSetAttributesSendsOnlyChangedAttributes() throws Exception {
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TYPE, "dev");
      model.setAttribute(Capability.ATTR_ID, destination.getId().toString());
      model.setAttribute(Capability.ATTR_ADDRESS, destination.getRepresentation());
      model.setAttribute("swit:state", "ON");
      model.setAttribute("dim:brightness", 50);
      context.putModel(model);
      context.setVariable("filterUnchanged", Boolean.TRUE);

      SendAction action =
            Actions
               .buildSendAction(Capability.CMD_SET_ATTRIBUTES)
               .withDestination(destination)
               .withAttribute("swit:state", "ON")      // already ON — should be filtered
               .withAttribute("dim:brightness", 100)    // different — should be sent
               .build();

      action.execute(context);

      PlatformMessage message = context.getMessages().poll();
      assertNotNull("Expected message for changed attributes", message);
      Map<String, Object> attrs = message.getValue().getAttributes();
      assertFalse("swit:state should have been filtered", attrs.containsKey("swit:state"));
      assertEquals(100, ((Number) attrs.get("dim:brightness")).intValue());

      assertNull(context.getMessages().poll());
   }

   @Test
   public void testSetAttributesSendsAllWhenModelNotFound() throws Exception {
      // No model in context — should send all attributes
      context.setVariable("filterUnchanged", Boolean.TRUE);

      SendAction action =
            Actions
               .buildSendAction(Capability.CMD_SET_ATTRIBUTES)
               .withDestination(destination)
               .withAttribute("swit:state", "ON")
               .build();

      action.execute(context);

      PlatformMessage message = context.getMessages().poll();
      assertNotNull("Expected message when model not found", message);
      assertEquals("ON", message.getValue().getAttributes().get("swit:state"));

      assertNull(context.getMessages().poll());
   }

   @Test
   public void testSetAttributesHandlesNumericTypeMismatch() throws Exception {
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TYPE, "dev");
      model.setAttribute(Capability.ATTR_ID, destination.getId().toString());
      model.setAttribute(Capability.ATTR_ADDRESS, destination.getRepresentation());
      model.setAttribute("dim:brightness", Integer.valueOf(100));
      context.putModel(model);
      context.setVariable("filterUnchanged", Boolean.TRUE);

      // Scene sends Double 100.0 but model stores Integer 100 — should match
      SendAction action =
            Actions
               .buildSendAction(Capability.CMD_SET_ATTRIBUTES)
               .withDestination(destination)
               .withAttribute("dim:brightness", Double.valueOf(100.0))
               .build();

      action.execute(context);

      assertNull("Expected no message when numeric values are equivalent",
            context.getMessages().poll());
   }

   @Test
   public void testSetAttributesSendsWhenCurrentValueIsNull() throws Exception {
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TYPE, "dev");
      model.setAttribute(Capability.ATTR_ID, destination.getId().toString());
      model.setAttribute(Capability.ATTR_ADDRESS, destination.getRepresentation());
      // swit:state is not set (null) — should still send
      context.putModel(model);
      context.setVariable("filterUnchanged", Boolean.TRUE);

      SendAction action =
            Actions
               .buildSendAction(Capability.CMD_SET_ATTRIBUTES)
               .withDestination(destination)
               .withAttribute("swit:state", "ON")
               .build();

      action.execute(context);

      PlatformMessage message = context.getMessages().poll();
      assertNotNull("Expected message when current value is null", message);
      assertEquals("ON", message.getValue().getAttributes().get("swit:state"));
   }

   @Test
   public void testSetAttributesNotFilteredWhenFilterUnchangedNotSet() throws Exception {
      // Without the filterUnchanged variable, all attributes should be sent
      // even if they match current state (default behavior for rules)
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TYPE, "dev");
      model.setAttribute(Capability.ATTR_ID, destination.getId().toString());
      model.setAttribute(Capability.ATTR_ADDRESS, destination.getRepresentation());
      model.setAttribute("swit:state", "ON");
      context.putModel(model);

      SendAction action =
            Actions
               .buildSendAction(Capability.CMD_SET_ATTRIBUTES)
               .withDestination(destination)
               .withAttribute("swit:state", "ON")
               .build();

      action.execute(context);

      PlatformMessage message = context.getMessages().poll();
      assertNotNull("Expected message when filterUnchanged is not set", message);
      assertEquals("ON", message.getValue().getAttributes().get("swit:state"));
   }

   @Test
   public void testSetAttributesNotFilteredWhenFilterUnchangedFalse() throws Exception {
      // With filterUnchanged explicitly false, all attributes should be sent
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TYPE, "dev");
      model.setAttribute(Capability.ATTR_ID, destination.getId().toString());
      model.setAttribute(Capability.ATTR_ADDRESS, destination.getRepresentation());
      model.setAttribute("swit:state", "ON");
      context.putModel(model);
      context.setVariable("filterUnchanged", Boolean.FALSE);

      SendAction action =
            Actions
               .buildSendAction(Capability.CMD_SET_ATTRIBUTES)
               .withDestination(destination)
               .withAttribute("swit:state", "ON")
               .build();

      action.execute(context);

      PlatformMessage message = context.getMessages().poll();
      assertNotNull("Expected message when filterUnchanged is false", message);
      assertEquals("ON", message.getValue().getAttributes().get("swit:state"));
   }

   @Test
   public void testNonSetAttributesCommandAlwaysSent() throws Exception {
      // Even if the model has matching values, non-SetAttributes commands
      // should always be sent (e.g., security arm, notifications)
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TYPE, "dev");
      model.setAttribute(Capability.ATTR_ID, destination.getId().toString());
      model.setAttribute(Capability.ATTR_ADDRESS, destination.getRepresentation());
      model.setAttribute("test:value", "test");
      context.putModel(model);

      SendAction action =
            Actions
               .buildSendAction("test:Action")
               .withDestination(destination)
               .withAttribute("test:value", "test")  // matches current state
               .build();

      action.execute(context);

      PlatformMessage message = context.getMessages().poll();
      assertNotNull("Non-SetAttributes commands should always be sent", message);
      assertEquals("test:Action", message.getMessageType());
   }
}

