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

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.addressing.HubAddr;
import com.iris.agent.util.EnvUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;

class SnoopingPort extends AbstractPort {
   private static final Logger log = LoggerFactory.getLogger(SnoopingPort.class);

   private final SnoopingPortHandler snoopingHandler;
   private final @Nullable AddressMatchingPort addressablePort;
   private final boolean gateway;

   SnoopingPort(Router parent, SnoopingPortHandler handler, @Nullable AddressMatchingPort addressablePort, String name, BlockingQueue<Message> queue) {
      this(parent, handler, addressablePort, name, queue, false);
   }

   SnoopingPort(Router parent, SnoopingPortHandler handler, @Nullable AddressMatchingPort addressablePort, String name, BlockingQueue<Message> queue, boolean gateway) {
      super(parent, handler, name, queue);
      this.addressablePort = addressablePort;
      this.gateway = gateway;
      this.snoopingHandler = handler;
   }

   @Override
   public void reply(PlatformMessage req, MessageBody rsp) {
      if (addressablePort != null) {
         addressablePort.reply(req, rsp);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void errorReply(PlatformMessage req, String code, String msg) {
      if (addressablePort != null) {
         addressablePort.errorReply(req, code, msg);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void errorReply(PlatformMessage req, Throwable cause) {
      if (addressablePort != null) {
         addressablePort.errorReply(req, cause);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void error(PlatformMessage req, String code, String msg) {
      if (addressablePort != null) {
         addressablePort.error(req, code, msg);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void error(PlatformMessage req, Throwable cause) {
      if (addressablePort != null) {
         addressablePort.error(req, cause);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public Object forward(HubAddr destination, PlatformMessage message) {
      if (addressablePort != null) {
         return addressablePort.forward(destination, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void forward(HubAddr destination, ProtocolMessage message) {
      if (addressablePort != null) {
         addressablePort.forward(destination, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void send(HubAddr destination, MessageBody message) {
      if (addressablePort != null) {
         addressablePort.send(destination, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void send(Address destination, MessageBody message) {
      if (addressablePort != null) {
         addressablePort.send(destination, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void send(HubAddr destination, MessageBody message, int ttl) {
      if (addressablePort != null) {
         addressablePort.send(destination, message, ttl);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void send(Address destination, MessageBody message, int ttl) {
      if (addressablePort != null) {
         addressablePort.send(destination, message, ttl);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void sendRequest(HubAddr destination, MessageBody message) {
      if (addressablePort != null) {
         addressablePort.sendRequest(destination, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void sendRequest(Address destination, MessageBody message) {
      if (addressablePort != null) {
         addressablePort.sendRequest(destination, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void sendRequest(HubAddr destination, MessageBody message, int ttl) {
      if (addressablePort != null) {
         addressablePort.sendRequest(destination, message, ttl);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void sendRequest(Address destination, MessageBody message, int ttl) {
      if (addressablePort != null) {
         addressablePort.sendRequest(destination, message, ttl);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void sendEvent(MessageBody message) {
      if (addressablePort != null) {
         addressablePort.sendEvent(message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void sendEvent(Address source, MessageBody message) {
      if (addressablePort != null) {
         addressablePort.sendEvent(source, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void sendEvent(MessageBody message, int ttl) {
      if (addressablePort != null) {
         addressablePort.sendEvent(message, ttl);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void sendEvent(Address source, MessageBody message, int ttl) {
      if (addressablePort != null) {
         addressablePort.sendEvent(source, message, ttl);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public <T> void send(HubAddr destination, Protocol<T> protocol, T message) {
      if (addressablePort != null) {
         addressablePort.send(destination, protocol, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void send(HubAddr destination, String protocol, byte[] message) {
      if (addressablePort != null) {
         addressablePort.send(destination, protocol, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public <T> void send(Address destination, Protocol<T> protocol, T message) {
      if (addressablePort != null) {
         addressablePort.send(destination, protocol, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void send(Address destination, String protocol, byte[] message) {
      if (addressablePort != null) {
         addressablePort.send(destination, protocol, message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void send(ProtocolMessage message) {
      if (addressablePort != null) {
         addressablePort.send(message);
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public void queue(PlatformMessage message) {
      sendPlatform(RouterUtils.getDestinationAddress(message), message, false);
   }

   @Override
   public void queue(ProtocolMessage message) {
      sendProtocol(RouterUtils.getDestinationAddress(message), message, false);
   }

   @Override
   public void queue(Object message) {
      sendCustom(this, message);
   }

   @Override
   public void queue(Object port, Object message) {
      sendCustom(port, message);
   }

   @Override
   public boolean isListenerOnly() {
      return (addressablePort == null);
   }

   @Override
   public boolean isListenAll() {
      return true;
   }

   @Override
   public void handle(HubAddr addr, PlatformMessage message) {
      if (addressablePort != null && 
          addr != null && 
          addr.getServiceId() != null && 
          Objects.equals(addr.getServiceId(),addressablePort.getServiceId())) {
         addressablePort.dispatch(message, false);
      } else {
         dispatch(message, true);
      }
   }

   @Override
   public void handle(HubAddr addr, ProtocolMessage message) {
      if (addressablePort != null && 
            addr != null && 
            addr.getProtocolId() != null &&
            Objects.equals(addr.getProtocolId(),addressablePort.getProtocolId())) {
         addressablePort.dispatch(message, false);
      } else {
         dispatch(message, true);
      }
   }

   @Override
   public void handle(@Nullable Object destination, Object message) {
      if (addressablePort != null) {
         Object dst = destination;
         if (dst == this) {
            dst = addressablePort;
         }

         addressablePort.dispatch(dst, message);
      } else {
         dispatch(destination, message);
      }
   }

   @Override
   public String toString() {
      return gateway ? "gateway" : "snooper";
   }

   @Override
   public Address getPlatformAddress() {
      if (addressablePort != null) {
         return addressablePort.getPlatformAddress();
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public Address getProtocolAddress() {
      if (addressablePort != null) {
         return addressablePort.getProtocolAddress();
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public Address getSendPlatformAddress() {
      if (addressablePort != null) {
         return addressablePort.getSendPlatformAddress();
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public @Nullable String getServiceId() {
      if (addressablePort != null) {
         return addressablePort.getServiceId();
      } else {
         return null;
      }
   }

   @Override
   public @Nullable String getProtocolId() {
      if (addressablePort != null) {
         return addressablePort.getProtocolId();
      } else {
         return null;
      }
   }
   
   @Override
   public void enqueue(@Nullable HubAddr addr, Message message, boolean snoop) throws InterruptedException {
      switch (message.getType()) {
      case PLATFORM:
         if (!snoop || snoopingHandler.isInterestedIn(((PlatformMessage)message.getMessage()))) {
            super.enqueue(addr, message, snoop);
         } else if (EnvUtils.isDevTraceEnabled(log)) {
            EnvUtils.devTrace(log,"{} snooper ignoring: {}", getName(), message);
         }
         break;

      case PROTOCOL:
         if (!snoop || snoopingHandler.isInterestedIn(((ProtocolMessage)message.getMessage()))) {
            super.enqueue(addr, message, snoop);
         } else if (EnvUtils.isDevTraceEnabled(log)) {
            EnvUtils.devTrace(log,"{} snooper ignoring: {}", getName(), message);
         }
         break;
      
      default:
         super.enqueue(addr, message, snoop);
         break;
      }
   }
}

