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
package com.iris.platform.rule.catalog.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.iris.common.rule.RuleContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PresenceSubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PresenceSubsystemModel;
import com.iris.messages.model.subs.SubsystemModel;

public class PresenceSelectorGenerator implements SelectorGenerator {
   private static final Logger logger = LoggerFactory.getLogger(PresenceSelectorGenerator.class);

   public PresenceSelectorGenerator() {
   }

   @Override
   public boolean isSatisfiable(RuleContext context) {
      Model model = context.getModelByAddress(Address.platformService(context.getPlaceId(), PresenceSubsystemCapability.NAMESPACE));
      if(model == null) {
         return false;
      }
      return SubsystemModel.getAvailable(model, false);
   }

   @Override
   public Selector generate(RuleContext context) {
      Model model = context.getModelByAddress(Address.platformService(context.getPlaceId(), PresenceSubsystemCapability.NAMESPACE));
      if(model == null) {
         throw new IllegalArgumentException("No presence subsystem is available");
      }
      List<Option> options = new ArrayList<Option>();
      
      addOptions(PresenceSubsystemModel.getPeopleAtHome(model, ImmutableSet.of()), context, options, PersonCapability.ATTR_FIRSTNAME, PersonCapability.ATTR_EMAIL);
      addOptions(PresenceSubsystemModel.getPeopleAway(model, ImmutableSet.of()), context, options, PersonCapability.ATTR_FIRSTNAME, PersonCapability.ATTR_EMAIL);
      addOptions(PresenceSubsystemModel.getDevicesAtHome(model, ImmutableSet.of()), context, options, DeviceCapability.ATTR_NAME);
      addOptions(PresenceSubsystemModel.getDevicesAway(model, ImmutableSet.of()), context, options, DeviceCapability.ATTR_NAME);
      
      ListSelector selector = new ListSelector();
      selector.setOptions(options);
      return selector;
   }

   private void addOptions(Set<String> addresses, RuleContext context, List<Option> options, String attribute, String... fallbacks) {
      for(String address: addresses) {
         Model model = context.getModelByAddress(Address.fromString(address));
         if(model == null) {
            logger.warn("Skipping address [{}] not found in context", address);
            continue;
         }
         String label = getLabel(model, attribute, fallbacks);
         if(label == null) {
            logger.warn("Skipping address [{}] because no label could be generated", address);
            continue;
         }
         options.add(new Option(label, address));
      }
   }
      
   @Nullable
   private String getLabel(Model model, String attribute, String... fallbacks) {
      Object value = model.getAttribute(attribute);
      for(int i=0; value == null && i<fallbacks.length; i++) {
         value = model.getAttribute(fallbacks[i]);
      }
      if(value == null) {
         return null;
      }
      else {
         return String.valueOf(value);
      }
   }

}

