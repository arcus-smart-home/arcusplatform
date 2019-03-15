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
package com.iris.oculus.modules.status;

import javax.swing.Action;

/**
 *
 */
public class MessageEvent {
   public enum Level {
      TRACE,
      DEBUG,
      INFO,
      WARN,
      ERROR
   }
   
   private Level level;
   private String message;
   private Action action;
   private Throwable error;
   
   public MessageEvent(Level level, String message) {
      this.level = level;
      this.message = message;
   }

   public MessageEvent(Level level, String message, Throwable error) {
      this.level = level;
      this.message = message;
      this.error = error;
   }

   public MessageEvent(Level level, String message, Action action) {
      this.level = level;
      this.message = message;
      this.action = action;
   }

   public MessageEvent(Level level, String message, Action action, Throwable error) {
      this.level = level;
      this.message = message;
      this.action = action;
      this.error = error;
   }

   public Level getLevel() {
      return level;
   }

   public String getMessage() {
      return message;
   }
   
   public boolean hasAction() {
      return action != null;
   }
   
   public Action getAction() {
      return action;
   }
   
   public boolean hasError() {
      return error != null;
   }
   
   public Throwable getError() {
      return error;
   }

   @Override
   public String toString() {
      return "MessageEvent [level=" + level + ", message=" + message + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((level == null) ? 0 : level.hashCode());
      result = prime * result + ((message == null) ? 0 : message.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MessageEvent other = (MessageEvent) obj;
      if (level != other.level) return false;
      if (message == null) {
         if (other.message != null) return false;
      }
      else if (!message.equals(other.message)) return false;
      return true;
   }
   
}

