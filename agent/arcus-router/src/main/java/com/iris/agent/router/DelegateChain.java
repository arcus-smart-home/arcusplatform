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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.protocol.ProtocolMessage;

class DelegateChain {
   private static final Logger log = LoggerFactory.getLogger(DelegateChain.class);

   private final List<Entry> entries;
   private final PortInternal basePort;
   private final PortHandler baseHandler;

   public DelegateChain(PortInternal basePort, PortHandler baseHandler) {
      this.entries = new CopyOnWriteArrayList<>();
      this.basePort = basePort;
      this.baseHandler = baseHandler;
   }

   @SuppressWarnings("unchecked")
   public void add(Predicate<?> filter, PortInternal port, PortHandler handler) {
      entries.add(new Entry((Predicate<Object>)filter,port,handler));
   }

   public void dispatch(PlatformMessage message, boolean snooped) {
      try {
         String type = message.getMessageType();
         for (Entry entry : entries) {
            if (entry.filter.apply(type)) {
               Object rsp = entry.handler.recv(entry.port, message);
               sendResponse(entry.port, message, rsp, snooped);
               return;
            }
         }

         Object rsp = baseHandler.recv(basePort, message);
         sendResponse(basePort, message, rsp, snooped);
      } catch (Throwable th) {
         if (!snooped && !basePort.isListenerOnly()) {
            if (message.isRequest()) {
               basePort.errorReply(message, th);
            } else if (!message.isError()) {
               basePort.error(message, th);
            }
         }
      }
   }

   public void dispatch(ProtocolMessage message, boolean snooped) {
      Object value = message.getValue();
      for (Entry entry : entries) {
         if (entry.filter.apply(value)) {
            entry.handler.recv(entry.port, message);
            return;
         }
      }

      baseHandler.recv(basePort, message);
   }

   public boolean dispatch(@Nullable Object destination, Object message) {
      // NOTE: This implementation doesn't use the defined filter
      //       because custom messages are dispatched back to the
      //       port that queued them up.
      for (Entry entry : entries) {
         if (entry.port == destination) {
            entry.handler.recv(entry.port, message);
            return true;
         }
      }

      if (basePort == destination) {
         baseHandler.recv(basePort, message);
         return true;
      }

      return false;
   }

   private void sendResponse(PortInternal port, PlatformMessage msg, @Nullable Object rsp, boolean snooped) {
      if (snooped) {
         // snoopers are not allowed to send responses
         return;
      }

      if (port.isListenerOnly()) {
         if (rsp != null) {
            log.trace("listen only port attempted to send response, discarding: {}", rsp);
         }

         return;
      }

      if (rsp == Port.HANDLED) {
         return;
      }

      if (msg.isError()) {
         // Don't send responses to errors
         return;
      }

      if (rsp instanceof MessageBody) {
         port.reply(msg, (MessageBody)rsp);
      } else if (rsp instanceof PlatformMessage) {
         port.send(msg);
      } else if (rsp == null) {
         if (msg.isResponseRequired()) {
            port.reply(msg, MessageBody.emptyMessage());
         }
      } else {
         log.warn("unknown response message, sending empty message instead: {}", rsp);
         port.reply(msg, MessageBody.emptyMessage());
      }
   }

   private static final class Entry {
      private final Predicate<Object> filter;
      private final PortInternal port;
      private final PortHandler handler;

      public Entry(Predicate<Object> filter, PortInternal port, PortHandler handler) {
         this.filter = filter;
         this.port = port;
         this.handler = handler;
      }
   }
}

