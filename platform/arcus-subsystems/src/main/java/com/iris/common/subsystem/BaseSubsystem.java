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
package com.iris.common.subsystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.Utils;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.key.NamespacedKey;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.Capability.AddTagsRequest;
import com.iris.messages.capability.Capability.RemoveTagsRequest;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.model.Version;
import com.iris.util.TypeMarker;

/**
 * 
 */
public abstract class BaseSubsystem<M extends SubsystemModel> extends AnnotatedSubsystem<M> {
   private static final TypeMarker<Set<String>> TYPE_STRING_SET = TypeMarker.setOf(String.class);
   
   private DefinitionRegistry registry;
   
   @Inject
   public void setDefinitionRegistry(DefinitionRegistry registry) {
      this.registry=registry;
   }
   
   protected boolean setIfNull(Model model, String attribute, Object value) {
      Object currentValue = model.getAttribute(attribute);
      if(currentValue == null) {
         model.setAttribute(attribute, value);
         return true;
      }
      else {
         return false;
      }
   }
   
   /* (non-Javadoc)
    * @see com.iris.common.subsystem.AnnotatedSubsystem#onAdded(com.iris.common.subsystem.SubsystemContext)
    */
   @Override
   protected void onAdded(SubsystemContext<M> context) {
      M model = context.model();
      model.setAttribute(Capability.ATTR_IMAGES, ImmutableMap.of());
      model.setAttribute(Capability.ATTR_INSTANCES, ImmutableMap.of());
      model.setAttribute(Capability.ATTR_TAGS, ImmutableSet.of());
      
      model.setName(getName());
      model.setAccount(context.getAccountId().toString());
      model.setPlace(context.getPlaceId().toString());
      model.setVersion(getVersion().getRepresentation());
      model.setHash(getHash());
      model.setAvailable(false);
      model.setState(SubsystemCapability.STATE_ACTIVE);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.AnnotatedSubsystem#onStarted(com.iris.common.subsystem.SubsystemContext)
    */
   @Override
   protected void onStarted(SubsystemContext<M> context) {
      super.onStarted(context);
      M model = context.model();
      
      // just in case there's been an upgrade
      model.setName(getName());
      if(!model.getVersion().equals(getVersion().getRepresentation())) {
         onUpgraded(context, Version.fromRepresentation(model.getVersion()));
         model.setVersion(getVersion().getRepresentation());
      }
      model.setHash(getHash());
   }

   protected void onUpgraded(SubsystemContext<M> context, Version oldVersion) {
      context.logger().debug("Upgrading from [{}] to [{}]...", oldVersion, getVersion());
   }

   protected void putInMap(
         Model model, 
         String attributeName,
         String key,
         Object value
   ) {
      Object v = model.getAttribute(attributeName);
      if(v == null) {
         model.setAttribute(attributeName, ImmutableMap.of(attributeName, value));
      }
      else if(v instanceof Map) {
         Map<String, Object> map = new HashMap<>((Map) v);
         map.put(key, value);
         model.setAttribute(attributeName, map);
      }
      else {
         throw new IllegalArgumentException("Attribute [" + attributeName + "] is not a map");
      }
   }

   protected boolean addAddressToSet(String address, String attribute, Model model) {
      Set<String> devices = model.getAttribute(TypeMarker.setOf(String.class), attribute).get();
      // skip a copy if we can
      if(devices.contains(address)) {
         return false;
      }
      
      Set<String> newDevices = new HashSet<>(devices);
      newDevices.add(address);
      model.setAttribute(attribute, newDevices);
      return true;
   }
   
   protected boolean removeAddressFromSet(String address, String attribute, Model model) {
      Set<String> devices = model.getAttribute(TypeMarker.setOf(String.class), attribute).get();
      if(!devices.contains(address)) {
         return false;
      }
      
      Set<String> newDevices = new HashSet<>(devices);
      newDevices.remove(address);
      model.setAttribute(attribute, newDevices);
      return true;
   }
   
   protected void setAttribute(String name, Object value, SubsystemContext<M> context) throws ErrorEventException {
      NamespacedKey key = NamespacedKey.parse(name);
      String attributeName = key.getNamedRepresentation();
      AttributeDefinition ad = registry.getAttribute(attributeName);
      if(ad == null || !ad.isWritable()) {
         context.logger().debug("Received set for unrecognized attribute {}", name);
         ErrorEvent error = Errors.unsupportedAttribute(name);
         throw new ErrorEventException(error);
      }
      
      if(key.isInstanced()) {
         if(!context.model().hasInstanceOf(key.getInstance(), key.getNamespace())) {
            context.logger().debug("Received set for unsupported instance attribute {}", name);
            ErrorEvent error = Errors.unsupportedAttribute(name);
            throw new ErrorEventException(error);
         }
      }
      else {
         if(!context.model().getCapabilities().contains(key.getNamespace())) {
            context.logger().debug("Received set for unsupported attribute {}", name);
            ErrorEvent error = Errors.unsupportedAttribute(name);
            throw new ErrorEventException(error);
         }
      }
      
      if(ad.hasMinMax()) {
         Errors.assertValidRequest(ad.isInRange(value), String.format("Attribute %s must be between %s and %s", ad.getName(), ad.getMin(), ad.getMax()));
      }
      context.model().setAttribute(name, ad.getType().coerce(value));
   }

   @Request(Capability.CMD_GET_ATTRIBUTES)
   public MessageBody getAttributes(
         @Named("names") Set<String> names,
         SubsystemContext<M> context
   ) {
      Map<String, Object> attributes;
      if(names == null || names.isEmpty()) {
         attributes = context.model().toMap();
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
         for(Map.Entry<String, Object> entry: context.model().toMap().entrySet()) {
            String name = entry.getKey();
            if(attributeNames.contains(name)) {
               attributes.put(name, entry.getValue());
            }
            else if(attributeNamespaces.contains(Utils.getNamespace(name))){
               attributes.put(name, entry.getValue());
            }
         }
      }

      filterPrivateAttributes(attributes);
      return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, attributes);
   }
   
   @Request(Capability.CMD_SET_ATTRIBUTES)
   public MessageBody setAttributes(PlatformMessage message, SubsystemContext<M> context) {
      MessageBody request = message.getValue();
      List<Map<String, Object>> errors = new ArrayList<> ();
      for(Map.Entry<String, Object> entry: request.getAttributes().entrySet()) {
         try {
            setAttribute(entry.getKey(), entry.getValue(), context);
         }
         catch(ErrorEventException e) {
         	context.logger().warn("Error setting attribute [{}]", entry.getKey(), e);
            errors.add(e.toErrorEvent().getAttributes());
         }
         catch(Exception e) {
            // TODO this should really be invalid parameter-value
         	context.logger().warn("Error setting attribute [{}]", entry.getKey(), e);
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
   
   @Request(AddTagsRequest.NAME)
   public void addTags(
         @Named(AddTagsRequest.ATTR_TAGS) Set<String> tags,
         SubsystemContext<M> context
   ) {
      Set<String> currentTags = context.model().getAttribute(TYPE_STRING_SET, Capability.ATTR_TAGS, ImmutableSet.<String>of());
      // make it modifiable
      Set<String> newTags = new HashSet<>(currentTags);
      newTags.addAll(tags);
      context.model().setAttribute(Capability.ATTR_TAGS, newTags);
   }
   
   @Request(RemoveTagsRequest.NAME)
   public void removeTags(
         @Named(RemoveTagsRequest.ATTR_TAGS) Set<String> tags,
         SubsystemContext<M> context
   ) {
      Set<String> currentTags = context.model().getAttribute(TYPE_STRING_SET, Capability.ATTR_TAGS, ImmutableSet.<String>of());
      // make it modifiable
      Set<String> newTags = new HashSet<>(currentTags);
      newTags.removeAll(tags);
      context.model().setAttribute(Capability.ATTR_TAGS, newTags);
   }

   private void filterPrivateAttributes(Map<String, Object> attributes) {
      Iterator<String> attributeName = attributes.keySet().iterator();
      while(attributeName.hasNext()) {
         if(attributeName.next().startsWith("_")) {
            attributeName.remove();
         }
      }
   }
   
   
	protected boolean addAddressIfMatches(Predicate<Model> predicate,
			Model model, Set<String> toAdd) {
		if (!predicate.apply(model)) {
			return false;
		}
		return toAdd.add(model.getAddress().getRepresentation());
	}

}

