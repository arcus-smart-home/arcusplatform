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
package com.iris.driver.groovy.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.context.CapabilityHandlerDefinition;
import com.iris.driver.groovy.context.RequestHandlerDefinition;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.MessageBody;
import com.iris.protocol.reflex.ReflexProtocol;

public class RequestEventHandler implements ContextualEventHandler<MessageBody> {
   private static final Logger log = LoggerFactory.getLogger(RequestEventHandler.class);
   private final List<RequestHandlerDefinition> handlers;

   public RequestEventHandler(List<RequestHandlerDefinition> handlers) {
      this.handlers = handlers;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, MessageBody event) throws Exception {
      boolean handled = false;
      for (RequestHandlerDefinition handler : handlers) {
         boolean matched = false;
         for (RequestHandlerDefinition.MatchRequest match : handler.getMatches()) {
            if (match.matches(event)) {
               matched = true;
               break;
            }
         }

         if (matched) {
            for (CapabilityHandlerDefinition.Action action : handler.getActions()) {
               try {
                  action.run(context, event);
               } catch (Exception ex) {
                  log.warn("failed to run action for request event handler match:", ex);
               }
            }

            if (handler.isForwarded()) {
               context.sendToDevice(ReflexProtocol.INSTANCE, event, -1);
            }

            MessageBody rsp = handler.getResponse();
            if (rsp != null) {
               context.respondToPlatform(rsp);
            }

            handled = true;
            break;
         }
      }

      return handled;
   }
}

