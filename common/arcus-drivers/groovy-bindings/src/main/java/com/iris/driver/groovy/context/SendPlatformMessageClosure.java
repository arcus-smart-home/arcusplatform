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
package com.iris.driver.groovy.context;

import groovy.lang.Closure;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

public class SendPlatformMessageClosure extends Closure<Object> {

   public SendPlatformMessageClosure(Object owner) {
      super(owner);
   }

   protected void doCall(String address, String messageType, Map<String,Object> attributes) {
      doCall(address, messageType, attributes, -1, false, null);
   }

   protected void doCall(String address, String messageType, Map<String,Object> attributes, boolean request) {
      doCall(address, messageType, attributes, -1, request, null);
   }

   protected void doCall(String address, String messageType, Map<String,Object> attributes, String correlationId) {
      doCall(address, messageType, attributes, -1, false, correlationId);
   }

   protected void doCall(String address, String messageType, Map<String,Object> attributes, boolean request, String correlationId) {
      doCall(address, messageType, attributes, -1, request, correlationId);
   }

   protected void doCall(String address, String messageType, Map<String,Object> attributes, int timeoutMs) {
      doCall(address, messageType, attributes, timeoutMs, false, null);
   }

   protected void doCall(String address, String messageType, Map<String,Object> attributes, int timeoutMs, boolean request) {
      doCall(address, messageType, attributes, timeoutMs, request, null);
   }

   protected void doCall(String address, String messageType, Map<String,Object> attributes, int timeoutMs, String correlationId) {
      doCall(address, messageType, attributes, timeoutMs, false, correlationId);
   }

   protected void doCall(String address, String messageType, Map<String,Object> attributes, int timeoutMs, boolean request, String correlationId) {
      if(StringUtils.isBlank(address)) {
         throw new IllegalArgumentException("Destination address must be specified");
      }
      if(StringUtils.isBlank(messageType)) {
         throw new IllegalArgumentException("Message type must be specified");
      }

      if(attributes == null) {
         attributes = new HashMap<>();
      }

      DeviceDriverContext context = GroovyContextObject.getContext();
      MessageBody body = MessageBody.buildMessage(messageType, attributes);
      PlatformMessage msg = PlatformMessage.buildMessage(
            body,
            context.getDriverAddress(),
            Address.fromString(address))
            .withCorrelationId(correlationId)
            .withTimeToLive(timeoutMs)
            .isRequestMessage(request)
            .withTimestamp(new Date())
            .withPlaceId(context.getPlaceId())
            .withPopulation(context.getPopulation())
            .create();
      context.sendToPlatform(msg);
   }
}

