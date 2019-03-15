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
package com.iris.messages.event;

import com.google.common.base.Preconditions;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

/**
 * Sent whenever a message is received.  This
 * event is fired before the model is updated,
 * which means for:
 *  - base:Added: The model will not yet exist in the context
 *  - base:ValueChange: The model will have the old
 *      attribute values
 *  - base:Removed: The model will still exist
 *      in the context.
 * 
 * To listen for those model change events its recommended
 * that the rule listen to {@link ModelAddedEvent}, {@link ModelRemovedEvent}
 * and {@link ModelChangedEvent}. 
 */
public class MessageReceivedEvent extends AddressableEvent {
   private PlatformMessage message;
   
   public MessageReceivedEvent(PlatformMessage message) {
      Preconditions.checkNotNull(message, "message may not be null");
      this.message = message;
   }
   
   @Override
   public Address getAddress() {
      return message.getSource();
   }
   
   public PlatformMessage getMessage() {
      return message;
   }

   @Override
   public String toString() {
      return "MessageReceivedEvent [message=" + message + "]";
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
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MessageReceivedEvent other = (MessageReceivedEvent) obj;
      if (message == null) {
         if (other.message != null) return false;
      }
      else if (!message.equals(other.message)) return false;
      return true;
   }

}

