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
package com.iris.driver.event;

import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

import java.util.Date;

public class ScheduledDriverEvent extends DriverEvent {
   private final String name;
   private final Object data;
   private final Address actor;
   private final long ts;

   ScheduledDriverEvent(String name, Object data, Address actor, long ts) {
      this.name = name;
      this.data = data;
      this.actor = actor;
      this.ts = ts;
   }

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * @return the data
    */
   public Object getData() {
      return data;
   }

   public Date getTimestamp() {
      return new Date(ts);
   }

   public Address getActor() {
      return this.actor;
   }

}

