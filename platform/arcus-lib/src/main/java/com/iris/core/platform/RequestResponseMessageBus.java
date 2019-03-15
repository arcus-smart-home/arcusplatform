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
package com.iris.core.platform;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.iris.core.messaging.MessageBus;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;
import com.iris.util.MdcContext.MdcContextReference;

public interface RequestResponseMessageBus<T extends Message> extends MessageBus<T> {
   static final Logger logger = LoggerFactory.getLogger(RequestResponseMessageBus.class);

   default ListenableFuture<Void> invokeAndSendResponse(T request, Callable<MessageBody> handler) {
      MessageBody response = invoke(request, handler);
      return sendResponse(request, response);
   }

   default ListenableFuture<Void> invokeAndSendResponse(T request, Function<T, ? extends MessageBody> handler) {
      MessageBody response = invoke(request, handler);
      return sendResponse(request, response);
   }

   /**
    * @deprecated Use {@link #invokeAndSendResponse(PlatformMessage, Callable)} but return {@link MessageBody#noResponse()}
    * in order to prevent message from being sent.
    * @param request
    * @param handler
    * @return
    */
   default ListenableFuture<Void> invokeAndSendIfNotNull(T request, Callable<Optional<MessageBody>> handler) {
      Optional<MessageBody> response = Optional.ofNullable( invoke(request, () -> handler.call().orElse(null)) );
      return sendResponse(request, response.orElse(MessageBody.noResponse()));
   }

   default MessageBody invoke(T request, Callable<MessageBody> method) {
      try (MdcContextReference context = Message.captureAndInitializeContext(request)) {
         try {
            return method.call();
         } catch (Exception e) {
            logger.warn("Error handling request [{}]", request, e);
            return Errors.fromException(e);
         }
      }
   }

   default MessageBody invoke(T request, Function<T, ? extends MessageBody> method) {
      try (MdcContextReference context = Message.captureAndInitializeContext(request)) {
         try {
            return method.apply(request);
         } catch (Exception e) {
            logger.warn("Error handling request [{}]", request, e);
            return Errors.fromException(e);
         }
      }
   }

   default ListenableFuture<Void> sendResponse(T request, MessageBody response) {
      if (MessageBody.noResponse().equals(response)) {
         return Futures.immediateFuture(null);
      }

      MessageBody body = response == null ? MessageBody.emptyMessage() : response;
      T message = getResponse(request, body);

      return send(message);
   }

   T getResponse(T req, MessageBody rsp);
}

