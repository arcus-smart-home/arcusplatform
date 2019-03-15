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
package com.iris.oculus;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.iris.client.event.handler.ClientEventHandler;
import com.iris.client.service.ClientPlatformService;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;

/**
 * 
 */
public class MockClientPlatformService implements ClientPlatformService {
   private ConcurrentLinkedQueue<Consumer<ClientMessage>> handlers = new ConcurrentLinkedQueue<>();
   private BlockingQueue<MessageBody> messages = new ArrayBlockingQueue<>(100);
   private boolean connected = true;
   private boolean connectionError = false;
   private String clientAddress = "CLNT:test:1";
   
   @Override
   public boolean connected() {
      return this.connected;
   }
   
   public void setConnected(boolean connected) {
      this.connected = connected;
   }

   @Override
   public boolean connectionError() {
      return connectionError;
   }
   
   public void setConnectionError(boolean connectionError) {
      this.connectionError = connectionError;
   }

   @Override
   public Future<Boolean> connect(String uri, String username, String password) {
      this.connected = true;
      return Futures.immediateFuture(true);
   }

   @Override
   public void disconnect() {
      this.connected = false;
   }

   @Override
   public boolean disconnected() {
      return !this.connected;
   }

   @Override
   public <H extends ClientEventHandler<?>> void registerEventHandler(Class<? extends MessageBody> type, H handler) {
      handlers.add((e) -> {
         if(type.isAssignableFrom(e.getPayload().getClass())) {
            handler.handleMessage(e);
         }
      });
   }

   @Override
   public ListenableFuture<MessageBody> request(String address, MessageBody message) throws IOException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void send(String address, MessageBody message) throws IOException {
      try {
         messages.put(message);
      }
      catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new IOException(e);
      }
   }
   
   public void sendToClient(String from, MessageBody payload) {
      ClientMessage message =
            ClientMessage
               .builder()
               .withDestination(clientAddress)
               .withSource(from)
               .withPayload(payload)
               .create()
               ;
      for(Consumer<ClientMessage> handler: handlers) {
         handler.accept(message);
      }
   }
   
   public BlockingQueue<MessageBody> getMessages() {
      return messages;
   }
   
   public MessageBody poll() {
      return messages.poll();
   }

}

