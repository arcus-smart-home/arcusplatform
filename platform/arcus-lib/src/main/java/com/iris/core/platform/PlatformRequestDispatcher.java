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
package com.iris.core.platform;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.util.IrisCollections;

/**
 *
 */
@Singleton
public class PlatformRequestDispatcher extends AbstractPlatformMessageHandler { 
   private final Map<String, PlatformRequestMessageHandler> dispatchTable;
   
   @Inject
   public PlatformRequestDispatcher(PlatformMessageBus platformBus, Collection<? extends PlatformRequestMessageHandler> handlers) {
      super(platformBus);

      Map<String, PlatformRequestMessageHandler> dispatchTable = new HashMap<String, PlatformRequestMessageHandler>(handlers.size() + 1);
      for(PlatformRequestMessageHandler handler: handlers) {
         dispatchTable.put(handler.getMessageType(), handler);
      }
      //setMessageBus(bus);
      this.dispatchTable = Collections.unmodifiableMap(dispatchTable);
   }
   
   public PlatformRequestDispatcher(PlatformMessageBus platformBus, Map<String, PlatformRequestMessageHandler> dispatchTable) {
      super(platformBus);
      this.dispatchTable = IrisCollections.unmodifiableCopy(dispatchTable); 
   }
   
   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageHandler#isAsync()
    */
   @Override
   protected boolean isAsync() {
      return true;
   }

   @Override
   protected MessageBody handleRequest(PlatformMessage message) throws Exception {
      String messageType = message.getMessageType();
      PlatformRequestMessageHandler handler = dispatchTable.get(messageType);
      if(handler == null) {
         handler = dispatchTable.get(MessageConstants.MSG_ANY_MESSAGE_TYPE);
         if (handler == null) {
            return super.handleRequest(message);
         }
      }
      return handler.handleMessage(message);
   }



}

