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
package com.iris.driver.service.handler;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.errors.Errors;

/**
 *
 */
public class MessageHandler implements DriverServiceRequestHandler {
   private final static Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);
   private final DriverExecutorRegistry consumers;

   @Inject
   public MessageHandler(DriverExecutorRegistry consumers) {
      this.consumers = consumers;
   }

   @Override
   public String getMessageType() {
      return MessageConstants.MSG_ANY_MESSAGE_TYPE;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.PlatformRequestMessageHandler#isAsync()
    */
   @Override
   public boolean isAsync() {
      return true;
   }

   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      DeviceDriverAddress address = (DeviceDriverAddress) message.getDestination();
      LOGGER.trace("DeviceCommandHandler received message [{}] to [{}]", message, address);
      DriverExecutor executor  = consumers.loadConsumer(address);
      Errors.assertPlaceMatches(message, executor.context().getPlaceId());
      Future<Void> handled = executor.fire(message);
      LOGGER.trace("DeviceCommandHandler {} {}", handled.isDone() ? "executed" : "queued", message);
      return null;
   }

}

