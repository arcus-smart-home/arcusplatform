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
package com.iris.driver.metadata;

import com.iris.driver.event.DriverEvent;

/**
 * Matcher for generic driver entry points.
 */
public class DriverEventMatcher extends EventMatcher {
   private final String eventName;
   private final Class<? extends DriverEvent> eventType;

   public DriverEventMatcher(Class<? extends DriverEvent> eventType) {
      this(eventType, null);
   }

   public DriverEventMatcher(Class<? extends DriverEvent> eventType, String eventName) {
      this.eventType = eventType;
      this.eventName = eventName;
   }

   public String getEventName() {
      return eventName;
   }

   public Class<? extends DriverEvent> getEventType() {
      return eventType;
   }

   @Override
   public String toString() {
      return eventName == null ? eventType.getSimpleName() + "Matcher" : eventName + "EventMatcher";
   }
}

