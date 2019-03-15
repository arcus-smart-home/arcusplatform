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
package com.iris.common.subsystem;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.name.Named;
import com.iris.annotation.Version;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.Capability.AddTagsRequest;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceCapability.DeviceConnectedEvent;
import com.iris.messages.capability.DeviceCapability.DeviceDisconnectedEvent;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnScheduledEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.util.IrisCollections;

/**
 * 
 */
public class TestAnnotatedSubsystem extends SubsystemTestCase<SubsystemModel> {
   TestSubsystem subsystem = new TestSubsystem();
   
   @Test
   public void testMetadata() {
      assertEquals(new com.iris.model.Version(1), subsystem.getVersion());
      assertEquals(SubsystemCapability.NAME, subsystem.getName());
      assertEquals(SubsystemCapability.NAMESPACE, subsystem.getNamespace());
      assertEquals(SubsystemModel.class, subsystem.getType());
      // can't hash an inner class right now...
//      assertTrue("Hash was: " + subsystem.getHash(), subsystem.getHash().length() > 0);
   }
   
   @Test
   public void testPersonAdded() {
      Address address = store.addModel(ModelFixtures.createPersonAttributes()).getAddress();
      
      ModelAddedEvent event = new ModelAddedEvent(address);
      subsystem.onEvent(event, context);
      
      assertEquals(1, subsystem.calls().size());
      MethodInvokation invokation = subsystem.calls().poll();
      assertEquals("onAdded", invokation.getName());
      assertEquals(Arrays.asList(event, context), invokation.getArgs());
      assertNoMessagesSent();
   }

   @Test
   public void testDeviceAdded() {
      Address address = store.addModel(ModelFixtures.createSwitchAttributes()).getAddress();
      
      ModelAddedEvent event = new ModelAddedEvent(address);
      subsystem.onEvent(event, context);
      
      assertEquals(2, subsystem.calls().size());
      Map<String, MethodInvokation> invokations = subsystem.callsToMap();
      {
         MethodInvokation invokation = invokations.get("onAdded");
         assertEquals(Arrays.asList(event, context), invokation.getArgs());
      }
      {
         MethodInvokation invokation = invokations.get("onDeviceAdded");
         assertEquals(Arrays.asList(address, store.getModelByAddress(address), context), invokation.getArgs());
      }

      assertNoMessagesSent();
   }

   @Test
   public void testPersonChanged() {
      Address address = store.addModel(ModelFixtures.createPersonAttributes()).getAddress();
      String oldName = (String) store.getModelByAddress(address).setAttribute(PersonCapability.ATTR_FIRSTNAME, "A new name");
      
      ModelChangedEvent event = ModelChangedEvent.create(address, PersonCapability.ATTR_FIRSTNAME, "A new name", oldName);
      subsystem.onEvent(event, context);
      
      assertEquals(1, subsystem.calls().size());
      MethodInvokation invokation = subsystem.calls().poll();
      assertEquals("onValueChanged", invokation.getName());
      assertEquals(Arrays.asList(event, context), invokation.getArgs());

      assertNoMessagesSent();
   }

   @Test
   public void testDeviceChanged() {
      Address address = store.addModel(ModelFixtures.createSwitchAttributes()).getAddress();
      String oldName = (String) store.getModelByAddress(address).setAttribute(DeviceCapability.ATTR_NAME, "A new name");
      
      ModelChangedEvent event = ModelChangedEvent.create(address, DeviceCapability.ATTR_NAME, "A new name", oldName);
      subsystem.onEvent(event, context);
      
      assertEquals(2, subsystem.calls().size());
      Map<String, MethodInvokation> invokations = subsystem.callsToMap();
      {
         MethodInvokation invokation = invokations.get("onValueChanged");
         assertEquals(Arrays.asList(event, context), invokation.getArgs());
      }
      {
         MethodInvokation invokation = invokations.get("onDeviceChanged");
         assertEquals(Arrays.asList(address, store.getModelByAddress(address), context), invokation.getArgs());
      }

      assertNoMessagesSent();
   }

   @Test
   public void testEmailChanged() {
      Address address = store.addModel(ModelFixtures.createPersonAttributes()).getAddress();
      String oldName = (String) store.getModelByAddress(address).setAttribute(PersonCapability.ATTR_EMAIL, "jd@gmail.com");
      
      ModelChangedEvent event = ModelChangedEvent.create(address, PersonCapability.ATTR_EMAIL, "jd@gmail.com", oldName);
      subsystem.onEvent(event, context);
      
      assertEquals(2, subsystem.calls().size());
      Map<String, MethodInvokation> invokations = subsystem.callsToMap();
      {
         MethodInvokation invokation = invokations.get("onValueChanged");
         assertEquals(Arrays.asList(event, context), invokation.getArgs());
      }
      {
         MethodInvokation invokation = invokations.get("onEmailChanged");
         assertEquals(Arrays.asList(), invokation.getArgs());
      }

      assertNoMessagesSent();
   }
   
   @Test
   public void testPersonRemoved() {
      Model model = new SimpleModel(ModelFixtures.createPersonAttributes());
      
      ModelRemovedEvent event = ModelRemovedEvent.create(model);
      subsystem.onEvent(event, context);
      
      assertEquals(1, subsystem.calls().size());
      MethodInvokation invokation = subsystem.calls().poll();
      assertEquals("onRemoved", invokation.getName());
      assertEquals(Arrays.asList(event, context), invokation.getArgs());
   
      assertNoMessagesSent();
   }

   @Test
   public void testDeviceRemoved() {
      Model model = new SimpleModel(ModelFixtures.createSwitchAttributes());
      
      ModelRemovedEvent event = ModelRemovedEvent.create(model);
      subsystem.onEvent(event, context);
      
      assertEquals(2, subsystem.calls().size());
      Map<String, MethodInvokation> invokations = subsystem.callsToMap();
      {
         MethodInvokation invokation = invokations.get("onRemoved");
         assertEquals(Arrays.asList(event, context), invokation.getArgs());
      }
      {
         MethodInvokation invokation = invokations.get("onDeviceRemoved");
         assertEquals(Arrays.asList(model.getAddress(), model, context), invokation.getArgs());
      }
   
      assertNoMessagesSent();
   }
   
   @Test
   public void testScheduleMessage() {
      ScheduledEvent event = new ScheduledEvent(context.model().getAddress(), System.currentTimeMillis());
      subsystem.onEvent(event, context);
      
      assertEquals(1, subsystem.calls().size());
      {
         MethodInvokation invokation = subsystem.calls().remove();
         assertEquals(Arrays.asList(event, context), invokation.getArgs());
      }
   
      assertNoMessagesSent();
   }
   
   @Test
   public void testAnyMessage() {
      PlatformMessage message =
            PlatformMessage
               .broadcast()
               .from("SERV:account:" + UUID.randomUUID().toString())
               .withPayload(Capability.EVENT_VALUE_CHANGE)
               .create();
      
      MessageReceivedEvent event = new MessageReceivedEvent(message);
      subsystem.onEvent(event, context);
      
      assertEquals(1, subsystem.calls().size());
      MethodInvokation invokation = subsystem.calls().poll();
      assertEquals("onAnyMessage", invokation.getName());
      assertEquals(Arrays.asList(event, context), invokation.getArgs());
   
      assertNoMessagesSent();
   }

   @Test
   public void testServiceMessage() {
      Address address = Address.platformService(AccountCapability.NAMESPACE);
      
      PlatformMessage message =
            PlatformMessage
               .broadcast()
               .from(address)
               .withPayload("test:Message")
               .create();
      
      MessageReceivedEvent event = new MessageReceivedEvent(message);
      subsystem.onEvent(event, context);
      
      assertEquals(2, subsystem.calls().size());
      Map<String, MethodInvokation> invokations = subsystem.callsToMap();
      {
         MethodInvokation invokation = invokations.get("onAnyMessage");
         assertEquals(Arrays.asList(event, context), invokation.getArgs());
      }
      {
         MethodInvokation invokation = invokations.get("onServiceMessage");
         assertEquals(Arrays.asList(message, context), invokation.getArgs());
      }
   
      assertNoMessagesSent();
   }

   @Test
   public void testDeviceMessage() {
      Model model = new SimpleModel(ModelFixtures.createSwitchAttributes());
      
      PlatformMessage message =
            PlatformMessage
               .broadcast()
               .from(model.getAddress())
               .withPayload(DeviceConnectedEvent.NAME)
               .create();
      
      MessageReceivedEvent event = new MessageReceivedEvent(message);
      subsystem.onEvent(event, context);
      
      assertEquals(2, subsystem.calls().size());
      Map<String, MethodInvokation> invokations = subsystem.callsToMap();
      {
         MethodInvokation invokation = invokations.get("onAnyMessage");
         assertEquals(Arrays.asList(event, context), invokation.getArgs());
      }
      {
         MethodInvokation invokation = invokations.get("onDeviceMessage");
         assertEquals(Arrays.asList(message.getSource(), message.getValue(), context), invokation.getArgs());
      }
   
      assertNoMessagesSent();
   }

   @Test
   public void testTypedMessage() {
      Model model = new SimpleModel(ModelFixtures.createSwitchAttributes());
      
      PlatformMessage message =
            PlatformMessage
               .broadcast()
               .from(model.getAddress())
               .withPayload(DeviceDisconnectedEvent.NAME)
               .create();
      
      MessageReceivedEvent event = new MessageReceivedEvent(message);
      subsystem.onEvent(event, context);
      
      assertEquals(3, subsystem.calls().size());
      Map<String, MethodInvokation> invokations = subsystem.callsToMap();
      {
         MethodInvokation invokation = invokations.get("onAnyMessage");
         assertEquals(Arrays.asList(event, context), invokation.getArgs());
      }
      {
         MethodInvokation invokation = invokations.get("onDeviceMessage");
         assertEquals(Arrays.asList(message.getSource(), message.getValue(), context), invokation.getArgs());
      }
      {
         MethodInvokation invokation = invokations.get("onDeviceDisconnected");
         assertEquals(Arrays.asList(), invokation.getArgs());
      }
   
      assertNoMessagesSent();
   }
   
   @Test
   public void testMessageBodyResponse() {
      PlatformMessage message =
         PlatformMessage
            .request(model.getAddress())
            .from(Address.clientAddress("server", "session"))
            .withPayload(Capability.CMD_GET_ATTRIBUTES)
            .create();
      
      subsystem.onEvent(new MessageReceivedEvent(message), context);
      
      assertEquals(1, subsystem.calls().size());
      MethodInvokation invokation = subsystem.calls().poll();
      assertEquals("getAttributes", invokation.getName());
      assertEquals(Arrays.asList(context), invokation.getArgs());

      assertResponses(1);
      MessageBody response = responses.getValue();
      assertEquals(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, response.getMessageType());
      assertEquals(model.toMap(), response.getAttributes());
   }

   @Test
   public void testVoidResponse() {
      Set<String> tags = IrisCollections.setOf("tag1", "tag2");
      PlatformMessage message =
         PlatformMessage
            .request(model.getAddress())
            .from(Address.clientAddress("server", "session"))
            .withPayload(
                  AddTagsRequest.NAME, 
                  ImmutableMap.<String, Object>of(AddTagsRequest.ATTR_TAGS, tags)
            )
            .create();
      
      subsystem.onEvent(new MessageReceivedEvent(message), context);
      
      assertEquals(1, subsystem.calls().size());
      MethodInvokation invokation = subsystem.calls().poll();
      assertEquals("addTags", invokation.getName());
      assertEquals(Arrays.asList(tags, context), invokation.getArgs());

      assertResponses(1);
      MessageBody response = responses.getValue();
      assertEquals(MessageBody.emptyMessage(), response);
   }
   
   @Test
   public void testErrorEventException() {
      PlatformMessage message =
         PlatformMessage
            .request(model.getAddress())
            .from(Address.clientAddress("server", "session"))
            .withPayload("test:ThrowErrorEvent")
            .create();
      
      subsystem.onEvent(new MessageReceivedEvent(message), context);
      
      assertEquals(1, subsystem.calls().size());
      MethodInvokation invokation = subsystem.calls().poll();
      assertEquals("throwErrorEvent", invokation.getName());
      assertEquals(Arrays.asList(), invokation.getArgs());

      assertResponses(1);
      ErrorEvent response = (ErrorEvent) responses.getValue();
      assertEquals(Errors.CODE_MISSING_PARAM, response.getCode());
   }
   
   @Test
   public void testCheckedException() {
      PlatformMessage message =
         PlatformMessage
            .request(model.getAddress())
            .from(Address.clientAddress("server", "session"))
            .withPayload("test:ThrowException")
            .create();
      
      subsystem.onEvent(new MessageReceivedEvent(message), context);
      
      assertEquals(1, subsystem.calls().size());
      MethodInvokation invokation = subsystem.calls().poll();
      assertEquals("throwException", invokation.getName());
      assertEquals(Arrays.asList(), invokation.getArgs());

      assertResponses(1);
      // check that it is an error event
      ErrorEvent response = (ErrorEvent) responses.getValue();
      assertEquals("Exception", response.getCode());
      // TODO enable this when we start masking exceptions
//      assertEquals(Errors.CODE_GENERIC, response.getCode());
   }
   
   @Test
   public void testMethodNotImplemented() {
      PlatformMessage message =
            PlatformMessage
               .request(model.getAddress())
               .from(Address.clientAddress("server", "session"))
               .withPayload("test:Unknown")
               .create();
         
      subsystem.onEvent(new MessageReceivedEvent(message), context);
      
      assertEquals(0, subsystem.calls().size());
      
      assertResponses(1);
      
      ErrorEvent response = (ErrorEvent) responses.getValue();
      assertEquals(Errors.CODE_INVALID_REQUEST, response.getCode());
   }
   
   protected void assertNoMessagesSent() {
      assertFalse(responses.hasCaptured());
   }
   
   protected void assertResponses(int count) {
      assertEquals(count, responses.getValues().size());
   }
   
   @Subsystem(SubsystemModel.class)
   @Version(1)
   public static class TestSubsystem extends AnnotatedSubsystem<SubsystemModel> {
      private Queue<MethodInvokation> calls = new ArrayBlockingQueue<>(100);
      
      public Queue<MethodInvokation> calls() {
         return calls;
      }
      
      public Map<String, MethodInvokation> callsToMap() {
         Map<String, MethodInvokation> map = new LinkedHashMap<>(calls.size() + 1);
         MethodInvokation invokation = calls().poll();
         while(invokation != null) {
            map.put(invokation.name, invokation);
            invokation = calls().poll();
         }
         return map;
      }
      
      @Request(Capability.CMD_GET_ATTRIBUTES)
      public MessageBody getAttributes(SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("getAttributes", context));
         return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, context.model().toMap());
      }
      
      @Request(AddTagsRequest.NAME)
      public void addTags(
            @Named(AddTagsRequest.ATTR_TAGS) Set<String> tags,
            SubsystemContext<SubsystemModel> context
      ) {
         calls.add(new MethodInvokation("addTags", tags, context));
      }
      
      @Request("test:ThrowException")
      public MessageBody throwException() throws Exception {
         calls.add(new MethodInvokation("throwException"));
         throw new Exception("Boom");
      }
            
      @Request("test:ThrowErrorEvent")
      public void throwErrorEvent() {
         calls.add(new MethodInvokation("throwErrorEvent"));
         throw new ErrorEventException(Errors.missingParam("parameter"));
      }
            
      @OnAdded
      public void onAdded(ModelAddedEvent event, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onAdded", event, context));
      }

      @OnAdded(query = "base:type = 'dev'")
      public void onDeviceAdded(Address address, Model model, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onDeviceAdded", address, model, context));
      }
      
      @OnValueChanged
      public void onValueChanged(ModelChangedEvent event, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onValueChanged", event, context));
      }
      
      @OnValueChanged(query = "base:type = 'dev'")
      public void onDeviceChanged(Address address, Model model, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onDeviceChanged", address, model, context));
      }

      @OnValueChanged(attributes = PersonCapability.ATTR_EMAIL)
      public void onEmailChanged() {
         calls.add(new MethodInvokation("onEmailChanged"));
      }

      @OnRemoved
      public void onRemoved(ModelRemovedEvent event, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onRemoved", event, context));
      }
      
      @OnRemoved(query = "base:type = 'dev'")
      public void onDeviceRemoved(Address address, Model model, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onDeviceRemoved", address, model, context));
      }
      
      @OnScheduledEvent
      public void onScheduled(ScheduledEvent event, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onScheduledEvent", event, context));
      }
      
      @OnMessage
      public void onAnyMessage(MessageReceivedEvent event, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onAnyMessage", event, context));
      }

      @OnMessage(from="SERV:account:")
      public void onServiceMessage(PlatformMessage message, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onServiceMessage", message, context));
      }

      @OnMessage(from="DRIV:dev:*")
      public void onDeviceMessage(Address source, MessageBody message, SubsystemContext<SubsystemModel> context) {
         calls.add(new MethodInvokation("onDeviceMessage", source, message, context));
      }

      @OnMessage(types=DeviceCapability.DeviceDisconnectedEvent.NAME)
      public void onDeviceDisconnected() {
         calls.add(new MethodInvokation("onDeviceDisconnected"));
      }

   }
   
   public static class MethodInvokation {
      String name;
      List<Object> args;
      
      MethodInvokation(String name, Object... args) {
         this.name = name;
         this.args = Arrays.asList(args);
      }

      /**
       * @return the name
       */
      public String getName() {
         return name;
      }

      /**
       * @param name the name to set
       */
      public void setName(String name) {
         this.name = name;
      }

      /**
       * @return the args
       */
      public List<Object> getArgs() {
         return args;
      }

      /**
       * @param args the args to set
       */
      public void setArgs(List<Object> args) {
         this.args = args;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((args == null) ? 0 : args.hashCode());
         result = prime * result + ((name == null) ? 0 : name.hashCode());
         return result;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (getClass() != obj.getClass()) return false;
         MethodInvokation other = (MethodInvokation) obj;
         if (args == null) {
            if (other.args != null) return false;
         }
         else if (!args.equals(other.args)) return false;
         if (name == null) {
            if (other.name != null) return false;
         }
         else if (!name.equals(other.name)) return false;
         return true;
      }
      
      
   }
}

