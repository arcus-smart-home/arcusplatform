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

import java.util.Date;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriverContext;

public interface AttributeBindingHandler {
   void processInitialAttributes(DeviceDriverContext context, Date now);
   void processDirtyAttributes(DeviceDriverContext context, AttributeMap dirty, Date now);

   public static enum NoBindingsHandler implements AttributeBindingHandler {
      INSTANCE;

      @Override
      public void processInitialAttributes(DeviceDriverContext context, Date now) {
      }

      @Override
      public void processDirtyAttributes(DeviceDriverContext context, AttributeMap dirty, Date now) {
      }
   }

   public static final class SingleBindingsHandler implements AttributeBindingHandler {
      private final AttributeKey<?> sourceKey;
      private final AttributeKey<? super Date> boundKey;

      public SingleBindingsHandler(AttributeKey<?> sourceKey, AttributeKey<? super Date> boundKey) {
         this.sourceKey = sourceKey;
         this.boundKey = boundKey;
      }

      @Override
      public void processInitialAttributes(DeviceDriverContext context, Date now) {
         if (context.getAttributeValue(sourceKey) != null) {
            context.setAttributeValue(boundKey, now);
         }
      }

      @Override
      public void processDirtyAttributes(DeviceDriverContext context, AttributeMap dirty, Date now) {
         if (dirty.containsKey(sourceKey)) {
            context.setAttributeValue(boundKey, now);
         }
      }
   }

   public static final class MultipleBindingsHandler implements AttributeBindingHandler {
      private final Iterable<? extends AttributeBindingHandler> delegates;

      public MultipleBindingsHandler(Iterable<? extends AttributeBindingHandler> delegates) {
         this.delegates = delegates;
      }

      @Override
      public void processInitialAttributes(DeviceDriverContext context, Date now) {
         for (AttributeBindingHandler delegate : delegates) {
            delegate.processInitialAttributes(context, now);
         }
      }

      @Override
      public void processDirtyAttributes(DeviceDriverContext context, AttributeMap dirty, Date now) {
         for (AttributeBindingHandler delegate : delegates) {
            delegate.processDirtyAttributes(context, dirty, now);
         }
      }
   }
}

