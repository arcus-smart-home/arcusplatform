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
package com.iris.driver.handler;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.event.ScheduledDriverEvent;
import com.iris.driver.handler.AbstractDispatchingHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.handler.ContextualEventHandlers;

public class DriverEventHandler
   extends AbstractDispatchingHandler<DriverEvent>
   implements ContextualEventHandler<DriverEvent>
{

   public static DriverEventHandler.Builder builder() {
      return new Builder();
   }

   protected DriverEventHandler(Map<String, ContextualEventHandler<? super DriverEvent>> handlers) {
      super(handlers);
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, DriverEvent event) throws Exception {
      if(event instanceof ScheduledDriverEvent) {
         if(deliver(event.getClass().getName() + "#" + ((ScheduledDriverEvent) event).getName(), context, event)) {
            return true;
         }
      }
      
      if(deliver(event.getClass().getName(), context, event)) {
         return true;
      }

      if(deliver(WILDCARD, context, event)) {
         return true;
      }

      return false;
   }

   public static class Builder extends AbstractDispatchingHandler.Builder<DriverEvent, DriverEventHandler> {

      public Builder addHandler(Class<? extends DriverEvent> eventType, String eventName, ContextualEventHandler<? super DriverEvent> handler) {
         if(eventType == null) {
            return addWildcardHandler(handler);
         }
         if(StringUtils.isEmpty(eventName)) {
            return addHandler(eventType, handler);
         }
         doAddHandler(eventType.getName() + "#" + eventName, handler);
         return this;
      }

      public Builder addHandler(Class<? extends DriverEvent> eventType, ContextualEventHandler<? super DriverEvent> handler) {
         if(eventType == null) {
            return addWildcardHandler(handler);
         }
         doAddHandler(eventType.getName(), handler);
         return this;
      }

      public Builder addWildcardHandler(ContextualEventHandler<? super DriverEvent> driverEventHandler) {
         doAddHandler(WILDCARD, driverEventHandler);
         return this;
      }

      @Override
      protected DriverEventHandler create(Map<String, ContextualEventHandler<? super DriverEvent>> handlers) {
         return new DriverEventHandler(handlers);
      }

      @Override
      protected ContextualEventHandler<DriverEvent> marshal(Iterable<ContextualEventHandler<? super DriverEvent>> handlers) {
         return ContextualEventHandlers.marshalSender(handlers);
      }

   }

}

