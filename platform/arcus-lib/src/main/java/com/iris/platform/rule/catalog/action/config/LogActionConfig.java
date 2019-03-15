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
package com.iris.platform.rule.catalog.action.config;

import java.util.Map;

import com.iris.common.rule.action.stateful.LogAction;

public class LogActionConfig extends BaseActionConfig{
   public static final String TYPE = "log";
   
   private String message;

   public LogActionConfig(String message) {
      this.message = message;
   }

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public LogAction createAction(Map<String, Object> variables) {
      return new LogAction(message);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((message == null) ? 0 : message.hashCode());
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
      LogActionConfig other = (LogActionConfig) obj;
      if (message == null) {
         if (other.message != null)
            return false;
      }else if (!message.equals(other.message))
         return false;
      return true;
   }

   @Override
   public String toString() {
	   return "LogActionConfig [message=" + message + "]";
   }
   
}

