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
import com.iris.driver.service.executor.DriverExecutors;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;

/**
 *
 */
public class EmitEventClosure extends Closure<Object> {

   public EmitEventClosure(Object owner) {
      super(owner);
   }

   protected void doCall(MessageBody attributes) {
      broadcast(attributes);
   }

   protected void doCall(String eventName) {
      MessageBody event = MessageBody.buildMessage(eventName, ImmutableMap.<String, Object>of());
      broadcast(event);
   }

   protected void doCall(String eventName, Map<String, Object> attributes) {
      MessageBody event = MessageBody.buildMessage(eventName, attributes);
      broadcast(event);
   }
   
   protected void doCall(EventDefinition definition) {
      doCall(definition.getName());
   }

   protected void doCall(EventDefinition definition, Map<String, Object> attributes) {
      doCall(definition.getName(), attributes);
   }

   protected void broadcast(MessageBody event) {
      // TODO we could verify the event name is recognized and attributes
      DriverExecutors.get().context().broadcast(event);
   }

}

