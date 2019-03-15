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

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.iris.device.model.EventDefinition;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.messages.MessageBody;
import com.iris.messages.errors.Errors;

/**
 *
 */
public class SendResponseClosure extends Closure<Object> {

   public SendResponseClosure(Object owner) {
      super(owner);
   }

   protected void doCall(MessageBody response) {
      respondToPlatform(response);
   }

   protected void doCall() {
      doCall(MessageBody.emptyMessage());
   }
   
   protected void doCall(String eventName) {
      doCall(eventName, ImmutableMap.<String, Object>of());
   }
   
   protected void doCall(String eventName, Map<String, Object> response) {
      MessageBody event = MessageBody.buildMessage(eventName, response);
      respondToPlatform(event);
   }

   protected void doCall(EventDefinition definition) {
      doCall(definition.getName());
   }

   protected void doCall(EventDefinition definition, Map<String, Object> attributes) {
      doCall(definition.getName(), attributes);
   }

   protected void doCall(Throwable error) {
      respondToPlatform(Errors.fromException(error));
   }

   protected void respondToPlatform(MessageBody response) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      context.respondToPlatform(response);
   }

}

