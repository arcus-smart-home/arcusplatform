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
package com.iris.core.platform.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.key.NamespacedKey;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;

/**
 * 
 */
@Singleton
public class SetAttributesModelRequestHandler implements ContextualRequestMessageHandler<Model> {
   private static Logger logger = LoggerFactory.getLogger(SetAttributesModelRequestHandler.class);
   
   private final DefinitionRegistry registry;
   
   @Inject
   public SetAttributesModelRequestHandler(DefinitionRegistry registry) {
      this.registry = registry;
   }
   
   @Override
   public String getMessageType() {
      return Capability.CMD_SET_ATTRIBUTES;
   }

   @Override
   public MessageBody handleRequest(Model model, PlatformMessage message) {
      MessageBody request = message.getValue();
      List<Map<String, Object>> errors = new ArrayList<> ();
      if(request.getAttributes() != null) {
	      for(Map.Entry<String, Object> entry: request.getAttributes().entrySet()) {
	         try {
	            setAttribute(model, entry.getKey(), entry.getValue());
	         }
	         catch(ErrorEventException e) {
	            errors.add(e.toErrorEvent().getAttributes());
	         }
	         catch(Exception e) {
	            // TODO this should really be invalid parameter-value
	            errors.add(Errors.invalidParam(entry.getKey()).getAttributes());
	         }
	      }
      }
      if(errors.isEmpty()) {
         return MessageBody.emptyMessage();
      }
      else {
         return MessageBody.buildMessage(Capability.EVENT_SET_ATTRIBUTES_ERROR, ImmutableMap.<String, Object>of("errors", errors));
      }
   }

   protected void setAttribute(Model model, String name, Object value) throws Exception {
      NamespacedKey key = NamespacedKey.parse(name);
      String attributeName = key.getNamedRepresentation();
      AttributeDefinition ad = registry.getAttribute(attributeName);
      if(ad == null || !ad.isWritable()) {
         logger.debug("Received set for unrecognized attribute {}", name);
         ErrorEvent error = Errors.unsupportedAttribute(name);
         throw new ErrorEventException(error);
      }
      
      if(key.isInstanced()) {
         if(!model.hasInstanceOf(key.getInstance(), key.getNamespace())) {
            logger.debug("Received set for unsupported instance attribute {}", name);
            ErrorEvent error = Errors.unsupportedAttribute(name);
            throw new ErrorEventException(error);
         }
      }
      else {
         if(!model.getCapabilities().contains(key.getNamespace())) {
            logger.debug("Received set for unsupported attribute {}", name);
            ErrorEvent error = Errors.unsupportedAttribute(name);
            throw new ErrorEventException(error);
         }
      }
         
      model.setAttribute(name, ad.getType().coerce(value));
   }
}

