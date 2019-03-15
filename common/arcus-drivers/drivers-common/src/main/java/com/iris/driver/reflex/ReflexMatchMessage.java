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

import com.iris.messages.MessageBody;

public final class ReflexMatchMessage implements ReflexMatch {
   private final MessageBody msg;

   public ReflexMatchMessage(MessageBody msg) {
      this.msg = msg;
   }

   public MessageBody getMessage() {
      return msg;
   }

   @Override
   public String toString() {
      return "ReflexMatchMessage [" +
         "msg=" + msg + 
         "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((msg == null) ? 0 : msg.hashCode());
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
      ReflexMatchMessage other = (ReflexMatchMessage) obj;
      if (msg == null) {
         if (other.msg != null)
            return false;
      } else if (!msg.equals(other.msg))
         return false;
      return true;
   }
}

