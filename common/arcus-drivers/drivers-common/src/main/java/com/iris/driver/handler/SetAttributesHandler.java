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
package com.iris.driver.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iris.device.model.AttributeDefinition;
import com.iris.driver.DeviceDriverContext;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.Errors;

/**
 *
 */
public class SetAttributesHandler implements ContextualEventHandler<MessageBody> {
   private List<SetAttributesConsumer> consumers;
   private Map<String, AttributeDefinition> attributes;

   public SetAttributesHandler(Map<String, AttributeDefinition> attributes, List<SetAttributesConsumer> providers) {
      this.attributes = attributes;
      this.consumers = providers;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, MessageBody event) throws Exception {

      List<ErrorEvent> errors = new ArrayList<ErrorEvent>();
      Map<String, Object> attributes = getAttributes(context, event, errors);
      if(attributes == null || attributes.isEmpty()) {
         sendErrors(context, errors);
         return true;
      }

      dispatchToConsumers(context, attributes);
      for(String name: attributes.keySet()) {
         errors.add(Errors.unsupportedAttribute(name));
      }
      sendErrors(context, errors);

      return true;
   }

   private void sendErrors(DeviceDriverContext context, List<ErrorEvent> errors) {
      // TODO this should just be SetAttributesResponse
      if(errors == null || errors.isEmpty()) {
         context.respondToPlatform(MessageBody.emptyMessage());
      }
      else {
         MessageBody event = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES + "Error", Collections.<String,Object>singletonMap("errors", errors));
         context.respondToPlatform(event);
      }
   }

   private void dispatchToConsumers(
         DeviceDriverContext context,
         Map<String, Object> attributes
   ) {
      for(SetAttributesConsumer consumer: consumers) {
         Map<String, Object> namespaced;
         String namespace = consumer.getNamespace();
         if(namespace == null) {
            namespaced = attributes;
         }
         else {
            namespaced = getAttributesFromNamespace(namespace, attributes);
         }
         if(namespaced == null) {
            continue;
         }

         try {
            Set<String> handledByNamespace = consumer.setAttributes(context, namespaced);
            attributes.keySet().removeAll(handledByNamespace);
         }
         catch(Exception e) {
            context.getLogger().error("Error invoking setAttributes", e);
         }
      }
   }

   private Map<String, Object> getAttributesFromNamespace(String namespace, Map<String, Object> attributes) {
      Map<String, Object> results = null;
      String prefix = namespace + ":";
      for(String name: attributes.keySet()) {
         if(name.startsWith(prefix)) {
            if(results == null) {
               results = new HashMap<>();
            }
            results.put(name, attributes.get(name));
         }
      }
      return results;
   }

   private Map<String, Object> getAttributes(DeviceDriverContext context, MessageBody event, List<ErrorEvent> errors) {
      Map<String, Object> attributes = event.getAttributes();
      if(attributes == null || attributes.isEmpty()) {
         // no-op
         return null;
      }

      Map<String, Object> result = new HashMap<>(attributes);
      for(String name: attributes.keySet()) {
         AttributeDefinition definition = this.attributes.get(name);
         ErrorEvent error = null;
         if(definition == null) {
            error = ErrorEvent.fromCode("NoSuchAttribute", "No attribute named " + name + " exists");
         }
         else if(!definition.isWritable()) {
            error = ErrorEvent.fromCode("ReadOnlyAttribute", "Attribute " + name + " can't be set");
         }
//         else if(!context.getSupportedAttributes().contains(definition.getKey())) {
//            error = ErrorEvent.fromCode("UnsupportedAttribute", "Attribute " + name + " is not supported by this device");
//         }

         if(error != null) {
            errors.add(error);
            result.remove(name);
         }
      }
      return result;
   }

}

