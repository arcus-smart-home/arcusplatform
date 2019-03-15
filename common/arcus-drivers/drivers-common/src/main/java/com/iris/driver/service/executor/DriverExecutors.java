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
package com.iris.driver.service.executor;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import org.slf4j.MDC;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.driver.event.DeviceAttributesUpdatedEvent;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.event.DriverStartedEvent;
import com.iris.driver.event.DriverStoppedEvent;
import com.iris.driver.event.DriverUpgradedEvent;
import com.iris.messages.ErrorEvent;
import com.iris.messages.Message;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;
import com.iris.protocol.ProtocolMessage;
import com.iris.util.MdcContext;

public class DriverExecutors {
   private static final ThreadLocal<DriverExecutor> ExecutorRef = new ThreadLocal<DriverExecutor>();
   
   // TODO DriverExecutor should probably be an abstract base class with this method
   public static void dispatch(Object event, DriverExecutor executor) {
      DriverExecutor old = ExecutorRef.get();
      // TODO throw an exception on old? I think just resetting is safe...
      ExecutorRef.set(executor);
      try(AutoCloseable context = MdcContext.captureMdcContext()) {
         MDC.put(MdcContext.MDC_TARGET, executor.context().getDriverAddress().getRepresentation());
         MDC.put(MdcContext.MDC_PLACE, String.valueOf(executor.context().getPlaceId()));
         if(event == null) {
            executor.context().getLogger().debug("Dropping null event");
            return;
         }
         
         MDC.put(MdcContext.MDC_TYPE, event.getClass().getSimpleName());
         if(event instanceof PlatformMessage) {
            try(AutoCloseable c = Message.captureAndInitializeContext((Message) event)) {
               executor.driver().handlePlatformMessage((PlatformMessage) event, executor.context());
            }
         }
         else if(event instanceof ProtocolMessage) {
            try(AutoCloseable c = Message.captureAndInitializeContext((Message) event)) {
               executor.driver().handleProtocolMessage((ProtocolMessage) event, executor.context());
            }
         }
         else if(event instanceof PlatformMessageTimeout) {
            PlatformMessage message = ((PlatformMessageTimeout) event).getMessage();
            executor.context().cancel(message);
         }
         else if(event instanceof DriverEvent) {
            if(event instanceof DriverStartedEvent) {
               executor.driver().onRestored(executor.context());
            }
            else if(event instanceof DriverUpgradedEvent) {
               DriverUpgradedEvent upgEvent = (DriverUpgradedEvent)event;
               executor.driver().onUpgraded(upgEvent, upgEvent.getOldDriverId(), executor.context());
            }
            else if(event instanceof DriverStoppedEvent) {
               executor.driver().onSuspended(executor.context());
            }
            else if(event instanceof DeviceAttributesUpdatedEvent) {
               DeviceAttributesUpdatedEvent attrs = (DeviceAttributesUpdatedEvent)event;
               executor.driver().onAttributesUpdated(executor.context(), attrs.getAttributes(), attrs.getReflexVersion(), attrs.isDeviceMessage());
            }
            else {
               executor.driver().handleDriverEvent((DriverEvent) event, executor.context());
            }
         }
         else if(event instanceof ErrorEvent) {
            executor.driver().handleError((ErrorEvent) event, executor.context());
         }
         else if(event instanceof Throwable) {
            executor.driver().handleError(Errors.fromException((Throwable) event), executor.context());
         }
         else {
            executor.context().getLogger().warn("Can't handle events of type [{}]", event.getClass());
         }
      }
      catch(Exception e) {
         executor.context().getLogger().warn("Error handling message [{}]", event, e);
      }
      finally {
         ExecutorRef.set(old);
      }
   }

   public static ListenableFuture<Void> dispatch(final Object event, final DriverExecutor executor, ExecutorService threadPool) {
      final SettableFuture<Void> result = SettableFuture.create();
      threadPool.submit(new Runnable() {
         @Override
         public void run() {
            try {
               dispatch(event, executor);
               result.set(null);
            }
            catch(Throwable t) {
               result.setException(t);
            }
         }
      });
      return result;
   }
   
   public static DriverExecutor get() throws IllegalStateException {
      DriverExecutor executor = ExecutorRef.get();
      if(executor == null) {
         throw new IllegalStateException("No driver context is currently set");
      }
      return executor;
   }
   
   public static boolean isDriverThread() {
      return ExecutorRef.get() != null;
   }
   
   public static boolean isDriverThread(DriverExecutor executor) {
      return Objects.equals(ExecutorRef.get(), executor);
   }
   
}

