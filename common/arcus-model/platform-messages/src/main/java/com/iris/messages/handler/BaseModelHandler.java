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
package com.iris.messages.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.Utils;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.key.NamespacedKey;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.util.TypeMarker;

/**
 * 
 */
public class BaseModelHandler {
   private static final TypeMarker<Set<String>> TYPE_STRING_SET = TypeMarker.setOf(String.class);
   private static final Logger logger = LoggerFactory.getLogger(BaseModelHandler.class);

   private DefinitionRegistry registry;
   
   @Inject
   public void setDefinitionRegistry(DefinitionRegistry registry) {
      this.registry=registry;
   }
   

   protected MessageBody getAttributes(Model model, Set<String> names) {
      Map<String, Object> attributes;
      if(names == null || names.isEmpty()) {
         attributes = model.toMap();
      }
      else {
         Set<String> attributeNames = new HashSet<>();
         Set<String> attributeNamespaces = new HashSet<>();
         for(String name: names) {
            if(Utils.isNamespaced(name)) {
               attributeNames.add(name);
            }
            else {
               attributeNamespaces.add(name);
            }
         }
         
         attributes = new HashMap<String, Object>();
         for(Map.Entry<String, Object> entry: model.toMap().entrySet()) {
            String name = entry.getKey();
            if(attributeNames.contains(name)) {
               attributes.put(name, entry.getValue());
            }
            else if(attributeNamespaces.contains(Utils.getNamespace(name))){
               attributes.put(name, entry.getValue());
            }
         }
      }

      return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, attributes);
   }
   
   protected MessageBody setAttributes(Model model, Map<String, Object> attributes) {
      if(attributes == null || attributes.isEmpty()) {
         return MessageBody.emptyMessage();
      }
      
      List<Map<String, Object>> errors = new ArrayList<> ();
      for(Map.Entry<String, Object> entry: attributes.entrySet()) {
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
      if(errors.isEmpty()) {
         return MessageBody.emptyMessage();
      }
      else {
         return MessageBody.buildMessage(Capability.EVENT_SET_ATTRIBUTES_ERROR, ImmutableMap.<String, Object>of("errors", errors));
      }
   }
   
   protected void addTags(Model model, Set<String> tags) {
      Set<String> currentTags = model.getAttribute(TYPE_STRING_SET, Capability.ATTR_TAGS, ImmutableSet.<String>of());
      // make it modifiable
      Set<String> newTags = new HashSet<>(currentTags);
      newTags.addAll(tags);
      model.setAttribute(Capability.ATTR_TAGS, newTags);
   }
   
   protected void removeTags(Model model, Set<String> tags) {
      Set<String> currentTags = model.getAttribute(TYPE_STRING_SET, Capability.ATTR_TAGS, ImmutableSet.<String>of());
      // make it modifiable
      Set<String> newTags = new HashSet<>(currentTags);
      newTags.removeAll(tags);
      model.setAttribute(Capability.ATTR_TAGS, newTags);
   }

   protected void setAttribute(Model model, String name, Object value) throws ErrorEventException {
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

