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
package com.iris.driver.reflex;

import java.util.Map;

public final class ReflexActionSendPlatform implements ReflexAction {
   private final String event;
   private final Map<String,Object> args;
   private final boolean response;
  
   public ReflexActionSendPlatform(String event, Map<String,Object> args, boolean response) {
      this.event = event;
      this.args = args;
      this.response = response;
   }

   public String getEvent() {
      return event;
   }

   public Map<String, Object> getArgs() {
      return args;
   }

   public boolean isResponse() {
      return response;
   }

   @Override
   public String toString() {
      return "ReflexActionSendPlatform [" +
         "event=" + event +
         ",args=" + args +
         ",response=" + response +
         "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((args == null) ? 0 : args.hashCode());
      result = prime * result + ((event == null) ? 0 : event.hashCode());
      result = prime * result + (response ? 0 : 1);
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ReflexActionSendPlatform other = (ReflexActionSendPlatform) obj;
      if (response != other.response)
         return false;
      if (args == null) {
         if (other.args != null)
            return false;
      } else if (!args.equals(other.args))
         return false;
      if (event == null) {
         if (other.event != null)
            return false;
      } else if (!event.equals(other.event))
         return false;
      return true;
   }
}

