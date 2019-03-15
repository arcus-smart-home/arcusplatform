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

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class ReflexActionLog implements ReflexAction {
   public static enum Level { TRACE, DEBUG, INFO, WARN, ERROR }
   public static enum Arg { MESSAGE }

   final Level level;
   final String msg;
   final List<Arg> arguments;

   public ReflexActionLog(Level level, String msg) {
      this(level, msg, ImmutableList.<Arg>of());
   }

   public ReflexActionLog(Level level, String msg, List<Arg> arguments) {
      this.level = level;
      this.msg = msg;
      this.arguments = arguments;
   }

   public Level getLevel() {
      return level;
   }

   public String getMsg() {
      return msg;
   }

   public List<Arg> getArguments() {
      return arguments;
   }

   @Override
   public String toString() {
      return "ReflexActionLog [" + 
         "level=" + level +
         ",msg=" + msg +
         ",arguments=" + arguments +
         "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((arguments == null) ? 0 : arguments.hashCode());
      result = prime * result + ((msg == null) ? 0 : msg.hashCode());
      result = prime * result + ((level == null) ? 0 : level.hashCode());
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
      ReflexActionLog other = (ReflexActionLog) obj;
      if (arguments == null) {
         if (other.arguments != null)
            return false;
      } else if (!arguments.equals(other.arguments))
         return false;
      if (msg == null) {
         if (other.msg != null)
            return false;
      } else if (!msg.equals(other.msg))
         return false;
      if (level != other.level)
         return false;
      return true;
   }
}

