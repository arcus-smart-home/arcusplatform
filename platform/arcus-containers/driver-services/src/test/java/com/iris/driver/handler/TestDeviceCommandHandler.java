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
package com.iris.driver.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.Utils;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.key.NamespacedKey;
import com.iris.core.messaging.MessagesModule;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.Fixtures;

/**
 *
 */
public class TestDeviceCommandHandler {
   PlatformDeviceDriverContext context;
   Address client;
   Address driver;

   @Before
   public void setUp() throws Exception {
      // nothing should touch context methods
      context = EasyMock.createNiceMock(PlatformDeviceDriverContext.class);
      EasyMock.replay(context);

      ServiceLocator.init(GuiceServiceLocator.create(
         Bootstrap
            .builder()
            .withModuleClasses(MessagesModule.class, AttributeMapTransformModule.class)
            .build()
            .bootstrap()
      ));

      client = Fixtures.createClientAddress();
      driver = Fixtures.createDeviceAddress();
   }

   @After
   public void tearDown() throws Exception {
      ServiceLocator.destroy();
   }

   protected PlatformMessage createNonCommandMessage() {
      return createMessage(MessageBody.buildMessage("test", new HashMap<String,Object>()));
   }

   protected PlatformMessage createCommandMessage(String namespace, String command, String instance) {
      MessageBody body = MessageBody.buildMessage(NamespacedKey.representation(namespace, command, instance), new HashMap<>());
      return createMessage(body);
   }

   protected PlatformMessage createCommandMessage(String namespace, String command) {
      MessageBody body = MessageBody.buildMessage(NamespacedKey.representation(namespace, command), new HashMap<>());
      return createMessage(body);
   }

   protected PlatformMessage createMessage(MessageBody body) {
      return PlatformMessage.createMessage(body, client, driver);
   }


   @Test
   public void testEmpty() throws Exception {
      MessageBodyHandler handler =
         MessageBodyHandler
            .builder()
            .build();

      assertFalse(handler.hasAnyHandlers());
      assertFalse(handler.hasWildcardHandler());

      assertFalse(handler.handleEvent(context, createNonCommandMessage()));
      assertFalse(handler.handleEvent(context, createCommandMessage("test", "command")));
   }

   @Test
   public void testWildcard() throws Exception {
      MessageBody command = MessageBody.buildMessage("test:command", new HashMap<>());
      PlatformMessage nonCommandMessage = createNonCommandMessage();

      ContextualEventHandler<MessageBody> wildcardCommandHandler =
            EasyMock.createMock(ContextualEventHandler.class);
      EasyMock
         .expect(wildcardCommandHandler.handleEvent(context, command))
         .andReturn(true)
         .once();
      EasyMock
         .expect(wildcardCommandHandler.handleEvent(context, nonCommandMessage.getValue()))
         .andReturn(false)
         .once();

      EasyMock.replay(wildcardCommandHandler);

      MessageBodyHandler handler =
         MessageBodyHandler
            .builder()
            .addWildcardHandler(wildcardCommandHandler)
            .build();

      assertTrue(handler.hasAnyHandlers());
      assertTrue(handler.hasWildcardHandler());

      assertFalse(handler.handleEvent(context, nonCommandMessage));
      assertTrue(handler.handleEvent(context, createMessage(command)));

      EasyMock.verify(wildcardCommandHandler);
   }

   @Test
   public void testNamespaceMatch() throws Exception {
      MessageBody command = MessageBody.buildMessage("test:command", new HashMap<>());

      PlatformMessage nonCommandMessage = createNonCommandMessage();

      ContextualEventHandler<MessageBody> namespaceCommandHandler =
            EasyMock.createMock(ContextualEventHandler.class);
      EasyMock
         .expect(namespaceCommandHandler.handleEvent(context, command))
         .andReturn(true)
         .once();

      EasyMock
         .expect(namespaceCommandHandler.handleEvent(context, nonCommandMessage.getValue()))
         .andReturn(false)
         .once();

      EasyMock.replay(namespaceCommandHandler);

      MessageBodyHandler handler =
         MessageBodyHandler
            .builder()
            .addHandler(NamespacedKey.parse("test"), namespaceCommandHandler)
            .build();

      assertTrue(handler.hasAnyHandlers());
      assertFalse(handler.hasWildcardHandler());

      assertFalse(handler.handleEvent(context, createNonCommandMessage()));
      assertFalse(handler.handleEvent(context, createCommandMessage("different", "command")));
      assertTrue(handler.handleEvent(context, createMessage(command)));

      EasyMock.verify(namespaceCommandHandler);
   }

   @Test
   public void testNamedMatch() throws Exception {
      MessageBody command = MessageBody.buildMessage("test:command", new HashMap<>());

      ContextualEventHandler<MessageBody> commandHandler =
            EasyMock.createMock(ContextualEventHandler.class);
      EasyMock
         .expect(commandHandler.handleEvent(context, command))
         .andReturn(true)
         .once();

      EasyMock.replay(commandHandler);

      MessageBodyHandler handler =
         MessageBodyHandler
            .builder()
            .addHandler(NamespacedKey.named("test", "command"), commandHandler)
            .build();

      assertTrue(handler.hasAnyHandlers());
      assertFalse(handler.hasWildcardHandler());

      assertFalse(handler.handleEvent(context, createNonCommandMessage()));
      assertFalse(handler.handleEvent(context, createCommandMessage("different", "command")));
      assertFalse(handler.handleEvent(context, createCommandMessage("test", "commander")));
      assertTrue(handler.handleEvent(context, createMessage(command)));

      EasyMock.verify(commandHandler);
   }

   @Test
   public void testInstanceMatch() throws Exception {
      MessageBody command = MessageBody.buildMessage("test:command:instance", new HashMap<>());

      ContextualEventHandler<MessageBody> commandHandler =
            EasyMock.createMock(ContextualEventHandler.class);
      EasyMock
         .expect(commandHandler.handleEvent(context, command))
         .andReturn(true)
         .once();

      EasyMock.replay(commandHandler);

      MessageBodyHandler handler =
         MessageBodyHandler
            .builder()
            .addHandler(NamespacedKey.instanced("test", "command", "instance"), commandHandler)
            .build();

      assertTrue(handler.hasAnyHandlers());
      assertFalse(handler.hasWildcardHandler());

      assertFalse(handler.handleEvent(context, createNonCommandMessage()));
      assertFalse(handler.handleEvent(context, createCommandMessage("different", "command", "instance")));
      assertFalse(handler.handleEvent(context, createCommandMessage("test", "different", "instance")));
      assertFalse(handler.handleEvent(context, createCommandMessage("test", "command", "different")));
      // non-instance
      assertFalse(handler.handleEvent(context, createCommandMessage("test", "command")));
      assertTrue(handler.handleEvent(context, createMessage(command)));

      EasyMock.verify(commandHandler);
   }

   @Test
   public void testBreakout() throws Exception {
      MessageBody command1 = MessageBody.buildMessage("different:command", new HashMap<>());
      MessageBody command2 = MessageBody.buildMessage("test:other", new HashMap<>());
      MessageBody command3 = MessageBody.buildMessage("test:command", new HashMap<>());

      PlatformMessage nonCommandMessage = createNonCommandMessage();

      ContextualEventHandler<MessageBody> exactMatchHandler1 =
            EasyMock.createMock("ExactMatch1", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> exactMatchHandler2 =
            EasyMock.createMock("ExactMatch2", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> namespaceHandler1 =
            EasyMock.createMock("Namespace1", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> namespaceHandler2 =
            EasyMock.createMock("Namespace2", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> wildcardHandler1 =
            EasyMock.createMock("Wildcard1", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> wildcardHandler2 =
            EasyMock.createMock("Wildcard2", ContextualEventHandler.class);

      EasyMock.expect(wildcardHandler1.handleEvent(context, command1)).andReturn(true).once();
      EasyMock.expect(wildcardHandler1.handleEvent(context, nonCommandMessage.getValue())).andReturn(false).once();
      EasyMock.expect(wildcardHandler2.handleEvent(context, nonCommandMessage.getValue())).andReturn(false).once();
      EasyMock.expect(namespaceHandler1.handleEvent(context, command2)).andReturn(true).once();
      EasyMock.expect(namespaceHandler1.handleEvent(context, nonCommandMessage.getValue())).andReturn(false).once();
      EasyMock.expect(namespaceHandler2.handleEvent(context, nonCommandMessage.getValue())).andReturn(false).once();
      EasyMock.expect(exactMatchHandler1.handleEvent(context, command3)).andReturn(true).once();

      EasyMock.replay(exactMatchHandler1, exactMatchHandler2, namespaceHandler1, namespaceHandler2, wildcardHandler1, wildcardHandler2);

      MessageBodyHandler handler =
            MessageBodyHandler
               .builder()
               .addWildcardHandler(wildcardHandler1)
               .addHandler(NamespacedKey.of("test"), namespaceHandler1)
               .addHandler(NamespacedKey.of("test", "command"), exactMatchHandler1)
               .addWildcardHandler(wildcardHandler2)
               .addHandler(NamespacedKey.of("test"), namespaceHandler2)
               .addHandler(NamespacedKey.of("test", "command"), exactMatchHandler2)
               .build();

      assertFalse(handler.handleEvent(context, createNonCommandMessage()));
      assertTrue(handler.handleEvent(context, createMessage(command1)));
      assertTrue(handler.handleEvent(context, createMessage(command2)));
      assertTrue(handler.handleEvent(context, createMessage(command3)));

      EasyMock.verify(exactMatchHandler1, exactMatchHandler2, namespaceHandler1, namespaceHandler2, wildcardHandler1, wildcardHandler2);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testFallThrough() throws Exception {
      MessageBody command1 = MessageBody.buildMessage("different:command", new HashMap<>());
      MessageBody command2 = MessageBody.buildMessage("test:other", new HashMap<>());
      MessageBody command3 = MessageBody.buildMessage("test:command", new HashMap<>());

      PlatformMessage nonCommandMessage = createNonCommandMessage();

      ContextualEventHandler<MessageBody> exactMatchHandler1 =
            EasyMock.createMock("ExactMatch1", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> exactMatchHandler2 =
            EasyMock.createMock("ExactMatch2", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> namespaceHandler1 =
            EasyMock.createMock("NamespaceMatch1", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> namespaceHandler2 =
            EasyMock.createMock("NamespaceMatch2", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> wildcardHandler1 =
            EasyMock.createMock("WildcardMatch1", ContextualEventHandler.class);
      ContextualEventHandler<MessageBody> wildcardHandler2 =
            EasyMock.createMock("WildcardMatch2", ContextualEventHandler.class);

      ContextualEventHandler<MessageBody> [] handlers = new ContextualEventHandler [] {
         exactMatchHandler1, exactMatchHandler2, namespaceHandler1, namespaceHandler2,
         wildcardHandler1, wildcardHandler2
      };
      for(int i=4; i<handlers.length; i++) {
         EasyMock
            .expect(handlers[i].handleEvent(context, command1))
            .andReturn(false)
            .once();
      }
      for(int i=2; i<handlers.length; i++) {
         EasyMock
            .expect(handlers[i].handleEvent(context, command2))
            .andReturn(false)
            .once();
         EasyMock
            .expect(handlers[i].handleEvent(context, nonCommandMessage.getValue()))
            .andReturn(false)
            .once();
      }
      for(int i=0; i<handlers.length; i++) {
         EasyMock
            .expect(handlers[i].handleEvent(context, command3))
            .andReturn(false)
            .once();
      }
      EasyMock.replay((Object [])handlers);

      MessageBodyHandler handler =
            MessageBodyHandler
               .builder()
               .addWildcardHandler(wildcardHandler1)
               .addHandler(NamespacedKey.of("test"), namespaceHandler1)
               .addHandler(NamespacedKey.of("test", "command"), exactMatchHandler1)
               .addWildcardHandler(wildcardHandler2)
               .addHandler(NamespacedKey.of("test"), namespaceHandler2)
               .addHandler(NamespacedKey.of("test", "command"), exactMatchHandler2)
               .build();

      assertFalse(handler.handleEvent(context, nonCommandMessage));
      assertFalse(handler.handleEvent(context, createMessage(command1)));
      assertFalse(handler.handleEvent(context, createMessage(command2)));
      assertFalse(handler.handleEvent(context, createMessage(command3)));

      EasyMock.verify((Object []) handlers);
   }

}

