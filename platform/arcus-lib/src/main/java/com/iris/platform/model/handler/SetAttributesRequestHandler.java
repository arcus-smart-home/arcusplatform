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
package com.iris.platform.model.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.key.NamespacedKey;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.PersistentModel;
import com.iris.platform.model.PersistentModelWrapper;
import com.iris.util.IrisAttributeLookup;

@Singleton
public class SetAttributesRequestHandler {
   private static final Logger logger = LoggerFactory.getLogger(SetAttributesRequestHandler.class);

   @Request(Capability.CMD_SET_ATTRIBUTES)
   public MessageBody setAttributes(PersistentModelWrapper<? extends PersistentModel> wrapper, MessageBody request) {
      List<Map<String, Object>> errors = new ArrayList<> ();
      for(Map.Entry<String, Object> entry: request.getAttributes().entrySet()) {
         String attribute = entry.getKey();
         try {
            AttributeDefinition definition = IrisAttributeLookup.definition(attribute);
            Preconditions.checkArgument(definition != null, "Attribute %s not found", attribute);
            Preconditions.checkArgument(definition.isWritable(), "Attribute %s is not writable", attribute);
            NamespacedKey key = NamespacedKey.parse(attribute);
            if(key.isInstanced()) {
               Preconditions.checkArgument(
                  wrapper
                     .model()
                     .getInstances()
                     .getOrDefault(key.getInstance(), ImmutableSet.of())
                     .contains(key.getNamespace()), 
                  "Instance %s does not exist or does not support namespace %s", key.getInstance(), key.getNamespace()
               );
            }
            else if(key.isNamed()) {
               Preconditions.checkArgument(wrapper.model().supports(key.getNamespace()), "Namespace %s is not supported", key.getNamespace());
            }
            else {
               throw new IllegalArgumentException("Attribute is not properly namespaced");
            }
            wrapper.model().setAttribute(attribute, entry.getValue());
         }
         catch(Exception e) {
            logger.debug("Invalid set attribute for attribute [{}]", attribute, e);
            errors.add(Errors.invalidParam(attribute).getAttributes());
         }
      }
      if(errors.isEmpty()) {
         wrapper.save();
         return MessageBody.emptyMessage();
      }
      else {
         return MessageBody.buildMessage(Capability.EVENT_SET_ATTRIBUTES_ERROR, ImmutableMap.<String, Object>of("errors", errors));
      }
   }
   
   protected void setAttribute(PersistentModelWrapper<? extends PersistentModel> wrapper, String key, Object value, Map<String, Object> changes) throws Exception {
      wrapper.model().setAttribute(key, value);
   }
}

