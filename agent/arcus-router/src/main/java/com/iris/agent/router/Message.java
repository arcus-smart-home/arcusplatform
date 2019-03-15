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
package com.iris.agent.router;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.addressing.HubAddr;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.protocol.ProtocolMessage;

final class Message {
   static enum Type { PLATFORM, PROTOCOL, CUSTOM, POISON };

   private final Type type;
   private @Nullable Address source;
   private @Nullable Object destination;
   private @Nullable Object message;
   private boolean forwarded;

   public Message(Type type, @Nullable Address source, @Nullable Object destination, @Nullable Object message, boolean forwarded) {
      this.type = type;
      this.source = source;
      this.destination = destination;
      this.message = message;
      this.forwarded = forwarded;
   }

   public Type getType() {
      return type;
   }

   public @Nullable Object getMessage() {
      return message;
   }

   public boolean isForwarded() {
      return forwarded;
   }

   public @Nullable Address getSource() {
      return source;
   }

   public @Nullable Object getDestination() {
      return destination;
   }

   public boolean isPoisonPill(Object target) {
      return type == Type.POISON && (this.message == null || this.message == target);
   }

   @Override
   public String toString() {
      return "Message [" +
         "fwd=" + forwarded +
         ",type=" + type +
         ",dst=" + destination +
         ",msg=" + message +
      "]";
   }
}

