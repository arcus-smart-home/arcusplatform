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
package com.iris.driver.groovy.control;

import java.util.Map;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.error.ErrorMessageHandler;
import com.iris.driver.handler.AbstractDispatchingHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.MessageBody;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.control.ControlProtocol;

public class ControlMessageHandler
   extends AbstractDispatchingHandler<MessageBody>
   implements ContextualEventHandler<ProtocolMessage> {

   private ErrorMessageHandler errorMessageHandler;

   public static ControlMessageHandler.Builder builder() {
      return new Builder();
   }

   protected ControlMessageHandler(Map<String, ContextualEventHandler<? super MessageBody>> handlers, ErrorMessageHandler errorMessageHandler) {
      super(handlers);
      this.errorMessageHandler = errorMessageHandler;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, ProtocolMessage event)
         throws Exception {
      MessageBody messageBody = event.getValue(ControlProtocol.INSTANCE);

      if("Error".equals(messageBody.getMessageType())) {
         if(errorMessageHandler != null && errorMessageHandler.handleEvent(context, event)) {
            return true;
         }
      }

      if (deliver(messageBody.getMessageType(), context, messageBody)) {
         return true;
      }

      if (deliver(WILDCARD, context, messageBody)) {
         return true;
      }

      return false;
   }

   public static class Builder extends AbstractDispatchingHandler.Builder<MessageBody, ControlMessageHandler> {

      private ErrorMessageHandler errorHandler;

      private Builder() {
      }

      public Builder addWildcardHandler(ContextualEventHandler<? super MessageBody> handler) {
         doAddHandler(WILDCARD, handler);
         return this;
      }

      public Builder addHandler(String messageType, ContextualEventHandler<? super MessageBody> handler) {
         doAddHandler(messageType, handler);
         return this;
      }

      public Builder addErrorHandler(ErrorMessageHandler errorHandler) {
         this.errorHandler = errorHandler;
         return this;
      }

      @Override
      protected ControlMessageHandler create(Map<String, ContextualEventHandler<? super MessageBody>> handlers) {
         return new ControlMessageHandler(handlers, errorHandler);
      }
   }
}

