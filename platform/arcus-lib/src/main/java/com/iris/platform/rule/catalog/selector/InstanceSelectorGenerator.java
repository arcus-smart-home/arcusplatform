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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iris.common.rule.RuleContext;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

/**
 * A selector generator that dynamically discovers available instance names
 * for a given capability namespace. When {@code dependsOn} is set, the
 * generator filters instances to only those from the selected device;
 * otherwise it aggregates instances across all models.
 */
public class InstanceSelectorGenerator implements SelectorGenerator {
   private final String capabilityNamespace;
   private final String dependsOn;

   public InstanceSelectorGenerator(String capabilityNamespace) {
      this(capabilityNamespace, null);
   }

   public InstanceSelectorGenerator(String capabilityNamespace, String dependsOn) {
      this.capabilityNamespace = capabilityNamespace;
      this.dependsOn = dependsOn;
   }

   @Override
   public boolean isSatisfiable(RuleContext context) {
      for (Model model : context.getModels()) {
         if (hasCapabilityInstances(model)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public Selector generate(RuleContext context) {
      return generateForModels(context.getModels());
   }

   @Override
   public Selector generate(RuleContext context, Map<String, String> selections) {
      if (dependsOn == null) {
         return generate(context);
      }

      String selectedAddress = (selections != null) ? selections.get(dependsOn) : null;
      if (selectedAddress == null) {
         // Dependency not yet resolved — return all instances with dependsOn hint
         // so old clients still get options while new clients know to re-resolve
         return generateForModels(context.getModels());
      }

      // Find the specific device model
      Address address = Address.fromString(selectedAddress);
      for (Model model : context.getModels()) {
         if (address.equals(model.getAddress())) {
            return generateForModels(Collections.singletonList(model));
         }
      }

      // Device not found — return empty list with dependsOn hint
      ListSelector selector = new ListSelector();
      selector.setDependsOn(dependsOn);
      return selector;
   }

   private Selector generateForModels(Iterable<Model> models) {
      Set<String> instanceNames = new LinkedHashSet<>();
      for (Model model : models) {
         if (hasCapabilityInstances(model)) {
            Map<String, Set<String>> instances = model.getInstances();
            for (Map.Entry<String, Set<String>> entry : instances.entrySet()) {
               if (entry.getValue() != null && entry.getValue().contains(capabilityNamespace)) {
                  instanceNames.add(entry.getKey());
               }
            }
         }
      }

      List<Option> options = new ArrayList<>();
      for (String name : instanceNames) {
         options.add(new Option(toDisplayLabel(name), name));
      }

      ListSelector selector = new ListSelector();
      selector.setOptions(options);
      if (dependsOn != null) {
         selector.setDependsOn(dependsOn);
      }
      return selector;
   }

   /**
    * Converts an instance key like "button1" or "top-left" into a
    * user-friendly label like "Button 1" or "Top Left".
    */
   static String toDisplayLabel(String instanceName) {
      // Insert space before trailing digits: "button1" -> "button 1"
      String spaced = instanceName.replaceAll("([a-zA-Z])(\\d)", "$1 $2");
      // Replace hyphens and underscores with spaces
      spaced = spaced.replace('-', ' ').replace('_', ' ');
      // Capitalize each word
      StringBuilder sb = new StringBuilder();
      for (String word : spaced.split(" ")) {
         if (word.isEmpty()) continue;
         if (sb.length() > 0) sb.append(' ');
         sb.append(Character.toUpperCase(word.charAt(0)));
         if (word.length() > 1) sb.append(word.substring(1));
      }
      return sb.toString();
   }

   @Override
   public String validate(RuleContext context, String value, Map<String, Object> allVariables) {
      if (dependsOn == null || value == null) {
         return null;
      }
      Object deviceAddr = allVariables.get(dependsOn);
      if (deviceAddr == null) {
         return null;
      }
      Address address = Address.fromString(String.valueOf(deviceAddr));
      for (Model model : context.getModels()) {
         if (address.equals(model.getAddress())) {
            Map<String, Set<String>> instances = model.getInstances();
            if (instances != null && instances.containsKey(value)) {
               Set<String> caps = instances.get(value);
               if (caps != null && caps.contains(capabilityNamespace)) {
                  return null; // valid
               }
            }
            return "Instance '" + value + "' does not exist on the selected device";
         }
      }
      return "Selected device not found";
   }

   private boolean hasCapabilityInstances(Model model) {
      Map<String, Set<String>> instances = model.getInstances();
      if (instances == null) {
         return false;
      }
      for (Map.Entry<String, Set<String>> entry : instances.entrySet()) {
         if (entry.getValue() != null && entry.getValue().contains(capabilityNamespace)) {
            return true;
         }
      }
      return false;
   }
}
