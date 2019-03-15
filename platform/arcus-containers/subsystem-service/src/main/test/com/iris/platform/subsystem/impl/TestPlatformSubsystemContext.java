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
package com.iris.platform.subsystem.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.SimpleModelStore;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.subsystem.SubsystemDao;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

/**
 * 
 */
@Mocks({ PlatformMessageBus.class, Scheduler.class, SubsystemDao.class })
public class TestPlatformSubsystemContext extends IrisMockTestCase {
   PlatformSubsystemContext<SubsystemModel> context;

   ModelEntity  entity;
   SimpleModelStore models;
   
   Capture<PlatformMessage> messages;
   
   @Inject SubsystemDao mockSubsystemDao;
   @Inject Scheduler mockScheduler;
   @Inject Listener<ScheduledEvent> mockEventListener;
   @Inject PlatformMessageBus platformBus;
   
   // have to do this @Provides to get the proper type signature
   @Provides
   Listener<ScheduledEvent> mockEventListener() {
      return EasyMock.createMock(Listener.class);
   }
   
   @Before
   public void createContext() throws Exception {
      entity = new ModelEntity(ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE));
      
      models = new SimpleModelStore();
      context =
            PlatformSubsystemContext
               .builder()
               .withAccountId(UUID.randomUUID())
               .withPlaceId(UUID.randomUUID())
               .withLogger(LoggerFactory.getLogger(TestPlatformSubsystemContext.class))
               .withModels(models)
               .withPlatformBus(platformBus)
               .withSubsystemDao(mockSubsystemDao)
               .withScheduler(mockScheduler)
               .withScheduledEventListener(mockEventListener)
               .build(SubsystemModel.class, entity);
   }
   
   @Before
   public void preparePlatformBus() {
      messages = Capture.newInstance(CaptureType.ALL);
      EasyMock
         .expect(platformBus.send(EasyMock.capture(messages)))
         .andReturn(Futures.immediateFuture(null))
         .anyTimes();
   }

   @Test
   public void testAddWithVariables() {
      expectCreateSubsystem();
      replay();
      
      Map<String, Object> values = entity.toMap();
      // convert to list for equals
      values.put(Capability.ATTR_CAPS, new ArrayList<>(entity.getCapabilities()));
      
      context.setVariable("var1", "val1");
      context.setVariable("var2", "val2");
      context.commit();
      
      PlatformMessage message = messages.getValue();
      assertEquals(Capability.EVENT_ADDED, message.getMessageType());
      assertEquals(values, message.getValue().getAttributes());
      // TODO assert a whole bunch more stuff
      
      verify();
   }

   @Test
   public void testVariablesOnlyDoesntSendValueChange() {
      entity.setCreated(new Date());
      entity.setModified(new Date());
      expectUpdateSubsystem();
      replay();
      
      Map<String, Object> values = entity.toMap();
      // convert to list for equals
      values.put(Capability.ATTR_CAPS, new ArrayList<>(entity.getCapabilities()));
      
      context.setVariable("var1", "val1");
      context.setVariable("var2", "val2");
      context.commit();
      
      assertFalse(messages.hasCaptured());
      
      verify();
   }

   @Test
   public void testChangeWithVariables() {
      entity.setCreated(new Date());
      entity.setModified(new Date());
      expectUpdateSubsystem();
      replay();
      
      Map<String, Object> values = entity.toMap();
      // convert to list for equals
      values.put(Capability.ATTR_CAPS, new ArrayList<>(entity.getCapabilities()));
      
      context.model().setState(SubsystemCapability.STATE_ACTIVE);
      context.setVariable("var1", "val1");
      context.setVariable("var2", "val2");
      context.commit();
      
      PlatformMessage message = messages.getValue();
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
      assertEquals(
            ImmutableMap.of(SubsystemCapability.ATTR_STATE, SubsystemCapability.STATE_ACTIVE), 
            message.getValue().getAttributes()
      );
      
      verify();
   }

   private void expectCreateSubsystem() {
      EasyMock
         .expect(mockSubsystemDao.save(entity))
         .andAnswer(() -> {
            ModelEntity copy = new ModelEntity(entity);
            copy.setCreated(new Date());
            copy.setModified(new Date());
            return copy;
         });
   }

   private void expectUpdateSubsystem() {
      EasyMock
      .expect(mockSubsystemDao.save(entity))
      .andAnswer(() -> {
         ModelEntity copy = new ModelEntity(entity);
         copy.setModified(new Date());
         return copy;
      });
   }
}

