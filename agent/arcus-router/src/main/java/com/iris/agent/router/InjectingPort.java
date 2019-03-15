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
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;


class InjectingPort extends AbstractPort {
   InjectingPort(Router parent, String name, BlockingQueue<Message> queue) {
      super(parent, null, name, queue);
   }

   @Override
   public void reply(PlatformMessage req, MessageBody rsp) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void errorReply(PlatformMessage req, String code, String msg) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void errorReply(PlatformMessage req, Throwable cause) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void error(PlatformMessage req, String code, String msg) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void error(PlatformMessage req, Throwable cause) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Object forward(HubAddr destination, PlatformMessage message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void forward(HubAddr destination, ProtocolMessage message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void send(HubAddr destination, MessageBody message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void send(Address destination, MessageBody message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void send(HubAddr destination, MessageBody message, int ttl) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void send(Address destination, MessageBody message, int ttl) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendRequest(HubAddr destination, MessageBody message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendRequest(Address destination, MessageBody message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendRequest(HubAddr destination, MessageBody message, int ttl) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendRequest(Address destination, MessageBody message, int ttl) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendEvent(MessageBody message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendEvent(Address source, MessageBody message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendEvent(MessageBody message, int ttl) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendEvent(Address source, MessageBody message, int ttl) {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T> void send(HubAddr destination, Protocol<T> protocol, T message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void send(HubAddr destination, String protocol, byte[] message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T> void send(Address destination, Protocol<T> protocol, T message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void send(Address destination, String protocol, byte[] message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void queue(PlatformMessage message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void queue(ProtocolMessage message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void queue(Object message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void queue(Object port, Object message) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isListenerOnly() {
      return true;
   }

   @Override
   public void handle(HubAddr addr, PlatformMessage message) {
      // Injecting ports don't handle messages
   }

   @Override
   public void handle(HubAddr addr, ProtocolMessage message) {
      // Injecting ports don't handle messages
   }


   @Override
   public void handle(@Nullable Object destination, Object message) {
      // Injecting ports don't handle messages
   }

   @Override
   public Address getPlatformAddress() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Address getProtocolAddress() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Address getSendPlatformAddress() {
      throw new UnsupportedOperationException();
   }
}

