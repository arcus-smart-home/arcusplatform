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
package com.iris.driver.service.consumer;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.iris.common.scheduler.ExecutorScheduler;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.service.executor.DefaultDriverExecutor;
import com.iris.messages.ErrorEvent;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.messages.model.Fixtures;
import com.iris.model.Version;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.test.StringProtocol;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;
import com.iris.util.IrisUUID;

/**
 *
 */
@Modules({InMemoryMessageModule.class})
@Mocks({PlaceDAO.class, PlacePopulationCacheManager.class, PersonPlaceAssocDAO.class, PersonDAO.class})
public class TestDefaultDriverExecutor extends IrisMockTestCase {
   static final int POLL_TIMEOUT_MS = 1000;
   static final int BACKLOG = 10;

   @Inject PlacePopulationCacheManager mockPopulationCacheMgr;
   
   Device device;
   SynchronousDeviceDriver driver;
   DeviceDriverContext context;
   DefaultDriverExecutor dispatcher;
   Scheduler scheduler;
   Executor executor;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      // every command gets its own thread so that we can
      // verify proper threading behavior
      executor = new Executor() {
         final AtomicInteger count = new AtomicInteger(0);
         @Override
         public void execute(Runnable command) {
            Thread thread = new Thread(command, "test-executor-" + count.getAndIncrement());
            thread.setDaemon(true);
            thread.start();
         }
      };

      scheduler = new ExecutorScheduler(Executors.newScheduledThreadPool(1), executor);
      device = Fixtures.createDevice();
      driver = new SynchronousDeviceDriver();
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      dispatcher = new DefaultDriverExecutor(driver, context, scheduler, BACKLOG);
   }

  
   protected PlatformMessage createPlatformMessage() {
      return
         PlatformMessage
            .builder()
            .from(Fixtures.createClientAddress())
            .to(Fixtures.createDeviceAddress())
            .withCorrelationId(IrisUUID.timeUUID().toString())
            .withPayload(MessageBody.ping())
            .create();
   }

   protected ProtocolMessage createProtocolMessage() {
      return
         ProtocolMessage
            .builder()
            .from(Fixtures.createProtocolAddress(StringProtocol.NAMESPACE))
            .to(Fixtures.createDeviceAddress())
            .withPayload(StringProtocol.INSTANCE, "test")
            .create();
   }

   /**
    * The outer future waits until the the event has been
    * "fired", the inner future is the result of that firing.
    * @param message
    * @return
    */
   protected Future<Future<Void>> submit(final Message message) {
      final SettableFuture<Future<Void>> ref = SettableFuture.create();
      executor.execute(new Runnable() {
         @Override
         public void run() {
            ref.set(dispatcher.fire(message));
         }
      });
      return ref;
   }

   /**
    * This tests that when multiple messages are queued up:
    *  1) Protocol messages should be processed first
    *  2) Each message is processed in the same thread
    * @throws Exception
    */
   @Test
   public void testProtocolPrioritizedOverPlatform() throws Exception {
      PlatformMessage plat1 = createPlatformMessage();
      PlatformMessage plat2 = createPlatformMessage();
      ProtocolMessage prot1 = createProtocolMessage();
      ProtocolMessage prot2 = createProtocolMessage();
      submit(plat1);
      // wait until the first thread has started executing
      driver.await();

      // make sure all these are queued up in order
      submit(plat2).get();
      submit(prot1).get();
      submit(prot2).get();


      String thread = dispatcher.getExecutorThreadName();
      assertNotNull(thread);
      assertSame(plat1, driver.respond(context));

      assertEquals(thread, dispatcher.getExecutorThreadName());
      assertSame(prot1, driver.advance());

      assertEquals(thread, dispatcher.getExecutorThreadName());
      assertSame(prot2, driver.advance());

      assertEquals(thread, dispatcher.getExecutorThreadName());
      assertSame(plat2, driver.respond(context));
   }

   @Test
   public void testIdleBetweenExecutions() throws Exception {
      PlatformMessage plat1 = createPlatformMessage();
      PlatformMessage plat2 = createPlatformMessage();

      Future<Future<Void>> f = submit(plat1);

      // wait until the first thread has started executing
      driver.await();
      String thread = dispatcher.getExecutorThreadName();
      assertNotNull(thread);
      assertSame(plat1, driver.respond(context));

      // wait until processing is complete
      f.get().get(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      assertFalse(dispatcher.isRunning());
      assertNull(dispatcher.getExecutorThreadName());

      f = submit(plat2);

      // wait until its being processed
      driver.await();
      assertTrue(dispatcher.isRunning());
      assertNotEquals(thread, dispatcher.getExecutorThreadName());
      assertSame(plat2, driver.respond(context));

      // wait until processing is complete
      f.get().get(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      assertFalse(dispatcher.isRunning());
      assertNull(dispatcher.getExecutorThreadName());
   }

   @Test
   public void testDefer() throws Exception {
      Future<Void> f = dispatcher.defer(createPlatformMessage());
      assertFalse(f.isDone());
      driver.advance();
      assertEquals(null, f.get());
   }

   @Test
   public void testDeferWithTimeout() throws Exception {
      Future<Void> f = dispatcher.defer(createPlatformMessage(), new Date(System.currentTimeMillis() + 100));
      assertFalse(f.isDone());
      assertFalse(f.isCancelled());
      driver.advance();
      assertEquals(null, f.get());
   }

   @Test
   public void testDeferNamed() throws Exception {
      PlatformMessage message = createPlatformMessage();
      dispatcher.defer("Test", message, new Date());
      assertEquals(message, driver.advance());
   }

   @Test
   public void testReplaceNamedEvent() throws Exception {
      PlatformMessage message1 = createPlatformMessage();
      DeviceConnectedEvent message2 = DriverEvent.createConnected(0);
      dispatcher.defer("Test", message1, new Date(System.currentTimeMillis() + 100));
      dispatcher.defer("Test", message2, new Date(System.currentTimeMillis() + 100));
      assertEquals(message2, driver.advance());
   }

   @Test
   public void testCancelNamedEvent() throws Exception {
      PlatformMessage message = createPlatformMessage();
      dispatcher.defer("Test", message, new Date(System.currentTimeMillis() + 100));
      dispatcher.cancel("Test");
      try {
         Object o = driver.advance();
         fail("Expected to timeout but got " + o);
      }
      catch(TimeoutException e) {
         // expected
      }
   }

   @Test
   public void testPlatformMessagesBuffered() throws Exception {
      PlatformMessage plat1 = createPlatformMessage();
      PlatformMessage plat2 = createPlatformMessage();
      PlatformMessage plat3 = createPlatformMessage();
      ProtocolMessage prot1 = createProtocolMessage();
      ProtocolMessage prot2 = createProtocolMessage();
      ProtocolMessage prot3 = createProtocolMessage();

      assertFalse(context.hasMessageContext());

      Future<Future<Void>> f = submit(plat1);

      // wait until the first thread has started executing
      driver.await();
      String thread = dispatcher.getExecutorThreadName();
      assertNotNull(thread);
      assertSame(plat1, driver.advance());

      // wait until processing is complete
      f.get().get(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      assertFalse(dispatcher.isRunning());
      assertNull(dispatcher.getExecutorThreadName());

      assertTrue(context.hasMessageContext());

      // now the next couple platform messages should be blocked
      // false means queued, not run
      assertFalse(submit(plat2).get().isDone());
      assertFalse(submit(plat3).get().isDone());

      assertFalse(dispatcher.isRunning());
      assertEquals(2, dispatcher.getQueuedMessageCount());

      // submit a protocol message to start the thread, and handle
      // plat1, should end up with plat2 in progress and plat3 queued
      {
         // this should start the thread up, and we'll respond to
         f = submit(prot1);
         driver.await();
         assertTrue(dispatcher.isRunning());
         assertEquals(prot1, driver.respond(context));
         assertEquals(plat2, driver.advance());

         // wait until its done, should have responded and cleared out the first context,
         // but now we're on to the second context
         assertTrue(f.get().isDone());
         assertFalse(dispatcher.isRunning());
         assertEquals(1, dispatcher.getQueuedMessageCount());
         assertTrue(context.hasMessageContext());
      }

      // submit a protocol message to start the thread, and handle
      // plat1, should end up with plat3 in progress and an empty queue
      {
         // this should start the thread up, and we'll respond to
         f = submit(prot2);
         driver.await();
         assertTrue(dispatcher.isRunning());
         assertEquals(prot2, driver.respond(context));
         assertEquals(plat3, driver.advance());

         // wait until its done, should have responded and cleared out the second context,
         // but now we're on to the third context
         assertTrue(f.get().isDone());
         assertFalse(dispatcher.isRunning());
         assertEquals(0, dispatcher.getQueuedMessageCount());
         assertTrue(context.hasMessageContext());
      }

      // and clear out the last message
      {
         // this should start the thread up, and we'll respond to
         f = submit(prot3);
         driver.await();
         assertTrue(dispatcher.isRunning());
         assertEquals(prot3, driver.respond(context));

         // wait until its done, should have responded and cleared out the second context,
         // but now we're on to the third context
         assertTrue(f.get().isDone());
         assertFalse(dispatcher.isRunning());
         assertEquals(0, dispatcher.getQueuedMessageCount());
         assertFalse(context.hasMessageContext());
      }
   }

   /**
    * DeviceDriver implementation that blocks in each event handling method
    * until {@link #advance()} or {@link #advance(Runnable)} are called.
    */
   public static class SynchronousDeviceDriver implements DeviceDriver {
      private final Exchanger<Object> exchange = new Exchanger<>();

      private DeviceDriverDefinition definition =
            DeviceDriverDefinition
               .builder()
               .withName("SynchronousDeviceDriver")
               .create();

      public Object advance() throws InterruptedException, TimeoutException {
         return advance(null);
      }

      /**
       * Blocks until the driver has started processing a message
       * @throws TimeoutException
       * @throws InterruptedException
       */
      public void await() throws InterruptedException, TimeoutException {
         final SettableFuture<Object> ref = SettableFuture.create();
         // pop the ref back into the exchanger on the other thread so that
         // we don't advance
         Runnable r = new Runnable() {
            @Override
            public void run() {
               try {
                  await(ref.get()).run();
               }
               catch(ExecutionException e) {
                  throw new RuntimeException(e.getCause());
               }
               catch(InterruptedException e) {
                  throw new RuntimeException(e);
               }
            }
         };
         Object event = exchange.exchange(r, POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
         ref.set(event);
      }

      /**
       * Calls advance() and responds to the current Platform
       * @param context
       * @return
       * @throws TimeoutException
       * @throws InterruptedException
       */
      public Object respond(DeviceDriverContext context) throws InterruptedException, TimeoutException {
         return respond(context, MessageBody.emptyMessage());
      }

      public Object respond(final DeviceDriverContext context, final MessageBody response) throws InterruptedException, TimeoutException {
         return advance(new Runnable(){
            @Override
            public void run() {
               context.respondToPlatform(response);
            }
         });
      }

      public Object advance(Runnable r) throws InterruptedException, TimeoutException {
         if(r == null) {
            r = new Runnable(){
               @Override
               public void run() {
                  // Do Nothing
               }
            };
         }
         return exchange.exchange(r, POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      }

      protected Runnable await(Object event) {
         try {
            return (Runnable) exchange.exchange(event, POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
         }
         catch(InterruptedException|TimeoutException e) {
            throw new RuntimeException(e);
         }
      }

      @Override
      public boolean supports(AttributeMap attributes) {
         return false;
      }

      @Override
      public DeviceDriverDefinition getDefinition() {
         return definition;
      }

      @Override
      public AttributeMap getBaseAttributes() {
         return null;
      }

      @Override
      public void onRestored(DeviceDriverContext context) {
         await(DriverEvent.driverStarted()).run();
      }

      @Override
      public void onUpgraded(DriverEvent event, DriverId previous, DeviceDriverContext context) {
         await(DriverEvent.driverUpgraded(previous)).run();
      }

      @Override
      public void onSuspended(DeviceDriverContext context) {
         await(DriverEvent.driverStopped()).run();
      }

      @Override 
      public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>,Object> attrs, Integer reflexVersion, boolean isDeviceMessage) {
      }

      @Override
      public void handleDriverEvent(DriverEvent event, DeviceDriverContext context) {
         await(event).run();
      }

      @Override
      public void handleProtocolMessage(ProtocolMessage message, DeviceDriverContext context) {
         Runnable r = await(message);
         r.run();
      }

      @Override
      public void handlePlatformMessage(PlatformMessage message, DeviceDriverContext context) {
         // TODO move this out of DeviceDriver?
         context.setMessageContext(message);
         await(message).run();
         // TODO add commit?
      }

      @Override
      public void handleError(ErrorEvent error, DeviceDriverContext context) {
         await(error).run();
      }

      @Override
      public DriverId getDriverId() {
         return new DriverId(getClass().getSimpleName(), Version.UNVERSIONED);
      }
   }
}

