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

import com.iris.agent.addressing.HubAddr;
import com.iris.agent.reflexes.HubReflexVersions;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;

abstract class AddressMatchingPort extends AbstractPort {
   public AddressMatchingPort(Router parent, PortHandler handler, String name, BlockingQueue<Message> queue) {
      super(parent, handler, name, queue);
   }

   protected Address getReplyAddress(PlatformMessage req) {
      Address addr = req.getDestination();
      return (addr == null) ? getSendPlatformAddress() : addr;
   }

   @Override
   public void reply(PlatformMessage req, MessageBody rsp) {
      PlatformMessage msg = PlatformMessage.buildResponse(req, rsp, getReplyAddress(req)).create();
      send(msg);
   }

   @SuppressWarnings("deprecation")
   @Override
   public void errorReply(PlatformMessage req, String code, String msg) {
      ErrorEvent error = ErrorEvent.fromCode(code, msg);
      PlatformMessage pmsg = PlatformMessage.buildResponse(req, error, getReplyAddress(req)).create();
      send(pmsg);
   }

   @SuppressWarnings("deprecation")
   @Override
   public void errorReply(PlatformMessage req, Throwable cause) {
      ErrorEvent error = ErrorEvent.fromException(cause);
      PlatformMessage pmsg = PlatformMessage.buildResponse(req, error, getReplyAddress(req)).create();
      send(pmsg);
   }

   @SuppressWarnings("deprecation")
   @Override
   public void error(PlatformMessage req, String code, String msg) {
      ErrorEvent error = ErrorEvent.fromCode(code, msg);
      PlatformMessage pmsg = PlatformMessage.buildResponse(req, error, getReplyAddress(req)).create();
      send(pmsg);
   }

   @SuppressWarnings("deprecation")
   @Override
   public void error(PlatformMessage req, Throwable cause) {
      ErrorEvent error = ErrorEvent.fromException(cause);
      PlatformMessage pmsg = PlatformMessage.buildResponse(req, error, getReplyAddress(req)).create();
      send(pmsg);
   }

   @Override
   public Object forward(HubAddr destination, PlatformMessage message) {
      PlatformMessage cloned = PlatformMessage.buildMessage(message.getValue(), message.getSource(), message.getDestination())
         .withTimeToLive(message.getTimeToLive())
         .withTimestamp(message.getTimestamp())
         .isRequestMessage(message.isRequest())
         .withoutCorrelationId()
         .create();

      sendForward(destination, cloned);
      return Port.HANDLED;
   }

   @Override
   public void forward(HubAddr destination, ProtocolMessage message) {
      sendForward(destination, message);
   }

   @Override
   public void send(HubAddr destination, MessageBody message) {
      send(destination, message, -1);
   }

   @Override
   public void send(Address destination, MessageBody message) {
      send(destination, message, -1);
   }

   @Override
   public void send(HubAddr destination, MessageBody message, int ttl) {
      PlatformMessage.Builder msg = PlatformMessage.buildMessage(message, getSendPlatformAddress(), null);
      if (ttl > 0) {
         msg.withTimeToLive(ttl);
      }

      send(destination, msg.create());
   }

   @Override
   public void send(Address destination, MessageBody message, int ttl) {
      PlatformMessage.Builder msg = PlatformMessage.buildMessage(message, getSendPlatformAddress(), destination);
      if (ttl > 0) {
         msg.withTimeToLive(ttl);
      }

      send(msg.create());
   }

   @Override
   public void sendRequest(HubAddr destination, MessageBody message) {
      sendRequest(destination, message, -1);
   }

   @Override
   public void sendRequest(Address destination, MessageBody message) {
      sendRequest(destination, message, -1);
   }

   @Override
   public void sendRequest(HubAddr destination, MessageBody message, int ttl) {
      PlatformMessage.Builder msg = PlatformMessage.buildRequest(message, getSendPlatformAddress(), null);
      if (ttl > 0) {
         msg.withTimeToLive(ttl);
      }

      send(destination, msg.create());
   }

   @Override
   public void sendRequest(Address destination, MessageBody message, int ttl) {
      PlatformMessage.Builder msg = PlatformMessage.buildRequest(message, getSendPlatformAddress(), destination);
      if (ttl > 0) {
         msg.withTimeToLive(ttl);
      }

      send(msg.create());
   }

   @Override
   public void sendEvent(MessageBody message) {
      sendEvent(message, -1);
   }

   @Override
   public void sendEvent(Address source, MessageBody message) {
      sendEvent(source, message, -1);
   }

   @Override
   public void sendEvent(MessageBody message, int ttl) {
      PlatformMessage msg = PlatformMessage.buildEvent(message, getSendPlatformAddress())
         .withTimeToLive(ttl)
         .create();
      send(msg);
   }

   @Override
   public void sendEvent(Address source, MessageBody message, int ttl) {
      PlatformMessage msg = PlatformMessage.buildEvent(message, source)
         .withTimeToLive(ttl)
         .create();
      send(msg);
   }

   @Override
   public <T> void send(HubAddr destination, Protocol<T> protocol, T message) {
      ProtocolMessage msg = ProtocolMessage.buildProtocolMessage(getProtocolAddress(), null, protocol, message)
         .withReflexVersion(HubReflexVersions.CURRENT)
         .create();
      send(destination, msg);
   }

   @Override
   public void send(HubAddr destination, String protocol, byte[] message) {
      ProtocolMessage msg = ProtocolMessage.buildProtocolMessage(getProtocolAddress(), null, protocol, message)
         .withReflexVersion(HubReflexVersions.CURRENT)
         .create();
      send(destination, msg);
   }

   @Override
   public <T> void send(Address destination, Protocol<T> protocol, T message) {
      ProtocolMessage msg = ProtocolMessage.buildProtocolMessage(getProtocolAddress(), destination, protocol, message)
         .withReflexVersion(HubReflexVersions.CURRENT)
         .create();
      send(msg);
   }

   @Override
   public void send(Address destination, String protocol, byte[] message) {
      ProtocolMessage msg = ProtocolMessage.buildProtocolMessage(getProtocolAddress(), destination, protocol, message)
         .withReflexVersion(HubReflexVersions.CURRENT)
         .create();
      send(msg);
   }

   @Override
   public void queue(PlatformMessage message) {
      send(message);
   }

   @Override
   public void queue(ProtocolMessage message) {
      send(message);
   }

   @Override
   public void queue(Object message) {
      sendCustom(this, message);
   }

   @Override
   public void queue(Object destination, Object message) {
      sendCustom(destination, message);
   }

   @Override
   public void handle(HubAddr addr, PlatformMessage message) {
      dispatch(message,false);
   }

   @Override
   public void handle(HubAddr addr, ProtocolMessage message) {
      dispatch(message,false);
   }

   @Override
   public void handle(@Nullable Object destination, Object message) {
      dispatch(destination, message);
   }
}

