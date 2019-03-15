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

import java.util.concurrent.BlockingQueue;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.iris.agent.addressing.HubAddr;
import com.iris.messages.PlatformMessage;
import com.iris.protocol.ProtocolMessage;

abstract class AbstractPort extends AbstractMessageProcessor implements PortInternal {
   private static final Logger log = LoggerFactory.getLogger(AbstractPort.class);

   private final Router parent;
   private final @Nullable DelegateChain chain;

   public AbstractPort(Router parent, @Nullable PortHandler handler, String name, BlockingQueue<Message> queue) {
      super(name, queue);

      this.parent = parent;
      if (handler == null) {
         this.chain = null;
      } else {
         this.chain = new DelegateChain(this,handler);
      }
   }

   @Override
   public Port delegate(PortHandler handler, String... messageTypes) {
      DelegateChain ch = chain;
      if (ch == null) {
         throw new UnsupportedOperationException("cannot delegate port with no handler");
      }

      Predicate<?> filter = RouterUtils.filter(messageTypes);
      DelegatePort port = new DelegatePort(this, handler, filter);
      ch.add(filter, port, handler);

      return port;
   }

   @Override
   public Port delegate(PortHandler handler, Class<?>... messageTypes) {
      DelegateChain ch = chain;
      if (ch == null) {
         throw new UnsupportedOperationException("cannot delegate port with no handler");
      }

      Predicate<?> filter = RouterUtils.filter(messageTypes);
      DelegatePort port = new DelegatePort(this, handler, filter);
      ch.add(filter, port, handler);

      return port;
   }

   protected void dispatch(PlatformMessage message, boolean snooped) {
      if (chain != null) {
         chain.dispatch(message, snooped);
      }
   }

   protected void dispatch(ProtocolMessage message, boolean snooped) {
      if (chain != null) {
         chain.dispatch(message, snooped);
      }
   }

   protected boolean dispatch(@Nullable Object destination, Object message) {
      if (chain != null) {
         return chain.dispatch(destination, message);
      }

      return false;
   }

   @Override
   public boolean isListenerOnly() {
      return false;
   }

   @Override
   public boolean isListenAll() {
      return false;
   }

   @Override
   public void handle(HubAddr addr, PlatformMessage message) {
   }

   @Override
   public void handle(HubAddr addr, ProtocolMessage message) {
   }

   @Override
   public void handle(@Nullable Object destination, Object message) {
   }

   @Override
   public void handle(Message message) throws Exception {
      Object msg = message.getMessage();
      if (msg == null) {
         log.debug("dropping null message");
         return;
      }

      Object dst = message.getDestination();
      if (dst == null) {
         switch (message.getType()) {
         case PLATFORM:
         case PROTOCOL:
            log.warn("dropping message not address to anyone: {}", msg);
            return;
         default:
            handle(dst, msg);
            return;
         }
      }

      switch (message.getType()) {
      case PLATFORM:
         handle((HubAddr)dst, (PlatformMessage)msg);
         break;

      case PROTOCOL:
         handle((HubAddr)dst, (ProtocolMessage)msg);
         break;

      case CUSTOM:
         handle(dst, msg);
         break;

      default:
         break;
      }
   }

   @Override
   public void send(PlatformMessage message) {
      send(RouterUtils.getDestinationAddress(message), message);
   }

   @Override
   public void send(ProtocolMessage message) {
      send(RouterUtils.getDestinationAddress(message), message);
   }

   @Override
   public void send(HubAddr addr, PlatformMessage message) {
      sendPlatform(addr, message, false);
   }

   public void sendForward(HubAddr addr, PlatformMessage message) {
      sendPlatform(addr, message, true);
   }

   @Override
   public void send(HubAddr addr, ProtocolMessage message) {
      sendProtocol(addr, message, false);
   }

   public void sendForward(HubAddr addr, ProtocolMessage message) {
      sendProtocol(addr, message, true);
   }

   protected void sendPlatform(HubAddr addr, PlatformMessage message, boolean forward) {
      parent.sendPlatform(addr, message, forward);
   }

   protected void sendProtocol(HubAddr addr, ProtocolMessage message, boolean forward) {
      parent.sendProtocol(addr, message, forward);
   }

   protected void sendCustom(Object addr, Object message) {
      parent.sendCustom(addr, message);
   }

   @Override
   public @Nullable String getServiceId() {
      return null;
   }

   @Override
   public @Nullable String getProtocolId() {
      return null;
   }
}

