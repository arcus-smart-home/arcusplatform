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

import com.google.common.base.Predicate;
import com.iris.agent.addressing.HubAddr;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;

class DelegatePort implements PortInternal {
   private final PortInternal parent;
   private final DelegateChain chain;

   public DelegatePort(PortInternal parent, PortHandler handler, Predicate<?> filter) {
      this.chain = new DelegateChain(this,handler);
      this.parent = parent;
   }

   @Override
   public Port delegate(PortHandler handler, String... messageTypes) {
      Predicate<?> filter = RouterUtils.filter(messageTypes);
      DelegatePort port = new DelegatePort(this, handler, filter);
      chain.add(filter, port, handler);

      return port;
   }

   @Override
   public Port delegate(PortHandler handler, Class<?>... messageTypes) {
      Predicate<?> filter = RouterUtils.filter(messageTypes);
      DelegatePort port = new DelegatePort(this, handler, filter);
      chain.add(filter, port, handler);

      return port;
   }

   @Override
   public void reply(PlatformMessage req, MessageBody rsp) {
      parent.reply(req, rsp);
   }

   @Override
   public void errorReply(PlatformMessage req, String code, String msg) {
      parent.errorReply(req, code, msg);
   }

   @Override
   public void errorReply(PlatformMessage req, Throwable cause) {
      parent.error(req, cause);
   }

   @Override
   public void error(PlatformMessage req, String code, String msg) {
      parent.error(req, code, msg);
   }

   @Override
   public void error(PlatformMessage req, Throwable cause) {
      parent.error(req, cause);
   }

   @Override
   public Object forward(HubAddr destination, PlatformMessage message) {
      return parent.forward(destination, message);
   }

   @Override
   public void forward(HubAddr destination, ProtocolMessage message) {
      parent.forward(destination, message);
   }

   @Override
   public void send(HubAddr destination, MessageBody message) {
      parent.send(destination, message);
   }

   @Override
   public void send(HubAddr destination, MessageBody message, int ttl) {
      parent.send(destination, message, ttl);
   }

   @Override
   public void send(HubAddr destination, PlatformMessage message) {
      parent.send(destination, message);
   }

   @Override
   public void send(Address destination, MessageBody message) {
      parent.send(destination, message);
   }

   @Override
   public void send(Address destination, MessageBody message, int ttl) {
      parent.send(destination, message, ttl);
   }

   @Override
   public void send(PlatformMessage message) {
      parent.send(message);
   }

   @Override
   public void sendRequest(HubAddr destination, MessageBody message) {
      parent.sendRequest(destination, message);
   }

   @Override
   public void sendRequest(Address destination, MessageBody message) {
      parent.sendRequest(destination, message);
   }

   @Override
   public void sendRequest(HubAddr destination, MessageBody message, int ttl) {
      parent.sendRequest(destination, message, ttl);
   }

   @Override
   public void sendRequest(Address destination, MessageBody message, int ttl) {
      parent.sendRequest(destination, message, ttl);
   }

   @Override
   public void sendEvent(MessageBody message) {
      parent.sendEvent(message);
   }

   @Override
   public void sendEvent(Address source, MessageBody message) {
      parent.sendEvent(source, message);
   }

   @Override
   public void sendEvent(MessageBody message, int ttl) {
      parent.sendEvent(message, ttl);
   }

   @Override
   public void sendEvent(Address source, MessageBody message, int ttl) {
      parent.sendEvent(source, message, ttl);
   }

   @Override
   public <T> void send(HubAddr destination, Protocol<T> protocol, T message) {
      parent.send(destination, protocol, message);
   }

   @Override
   public void send(HubAddr destination, String protocol, byte[] message) {
      parent.send(destination, protocol, message);
   }

   @Override
   public void send(HubAddr addr, ProtocolMessage message) {
      parent.send(addr, message);
   }

   @Override
   public <T> void send(Address destination, Protocol<T> protocol, T message) {
      parent.send(destination, protocol, message);
   }

   @Override
   public void send(Address destination, String protocol, byte[] message) {
      parent.send(destination, protocol, message);
   }

   @Override
   public void send(ProtocolMessage message) {
      parent.send(message);
   }

   @Override
   public void queue(PlatformMessage msg) {
      parent.queue(msg);
   }

   @Override
   public void queue(ProtocolMessage msg) {
      parent.queue(msg);
   }

   @Override
   public void queue(Object custom) {
      parent.queue(this,custom);
   }

   @Override
   public void queue(Object destination, Object message) {
      parent.queue(destination, message);
   }

   @Override
   public boolean isListenerOnly() {
      return parent.isListenerOnly();
   }

   @Override
   public boolean isListenAll() {
      return parent.isListenAll();
   }

   @Override
   public void handle(@Nullable HubAddr addr, PlatformMessage message) {
   }

   @Override
   public void handle(@Nullable HubAddr addr, ProtocolMessage message) {
   }

   @Override
   public void handle(Object destination, Object message) {
   }

   @Override
   public String getName() {
      return parent.getName();
   }

   @Override
   public Address getPlatformAddress() {
      return parent.getPlatformAddress();
   }

   @Override
   public Address getProtocolAddress() {
      return parent.getProtocolAddress();
   }

   @Override
   public Address getSendPlatformAddress() {
      return parent.getSendPlatformAddress();
   }

   @Override
   public void enqueue(@Nullable HubAddr addr, Message message, boolean snoop) throws InterruptedException {
      parent.enqueue(addr, message, snoop);
   }

   @Override
   public @Nullable String getServiceId() {
      return parent.getServiceId();
   }

   @Override
   public @Nullable String getProtocolId() {
      return parent.getProtocolId();
   }
}

