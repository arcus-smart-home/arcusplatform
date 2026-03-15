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
package com.iris.platform.scene.resolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.ActionList;
import com.iris.common.rule.action.SendAction;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.Model;
import com.iris.messages.type.ActionSelector;

/**
 * Scene resolver for multi-switch/multi-relay devices. Generates per-instance
 * On/Off selectors so that each relay or outlet can be controlled independently
 * within a scene.
 *
 * Only matches devices that have multiple instanced Switch capabilities
 * (e.g., dual-relay, dual-outlet devices).
 */
public class MultiSwitchResolver extends BaseResolver {

   public MultiSwitchResolver() {
      super("multiswitches", "Control Individual Switches", "light");
   }

   @Override
   protected List<ActionSelector> resolve(ActionContext context, Model model) {
      List<String> switchInstances = getSwitchInstances(model);
      if(switchInstances.size() < 2) {
         return ImmutableList.of();
      }

      List<ActionSelector> selectors = new ArrayList<>();
      for(String instance : switchInstances) {
         ActionSelector selector = new ActionSelector();
         selector.setName(instance);
         selector.setType(ActionSelector.TYPE_GROUP);

         List<List<Object>> values = new ArrayList<>();
         values.add(ImmutableList.of("ON"));
         values.add(ImmutableList.of("OFF"));
         selector.setValue(values);

         selectors.add(selector);
      }
      return selectors;
   }

   @Override
   public Action generate(ActionContext context, Address target, Map<String, Object> variables) {
      Model model = context.getModelByAddress(target);
      List<String> switchInstances = getSwitchInstances(model);

      Map<String, Object> attributes = new HashMap<>();
      for(String instance : switchInstances) {
         String value = (String) variables.get(instance);
         if(value != null) {
            attributes.put(SwitchCapability.ATTR_STATE + ":" + instance, value);
         }
      }

      ActionList.Builder builder = new ActionList.Builder();
      if(!attributes.isEmpty()) {
         builder.addAction(new SendAction(
            Capability.CMD_SET_ATTRIBUTES,
            Functions.constant(target),
            attributes
         ));
      }
      return builder.build();
   }

   private List<String> getSwitchInstances(Model model) {
      List<String> result = new ArrayList<>();
      if(model == null) {
         return result;
      }
      Map<String, Set<String>> instances = model.getInstances();
      if(instances == null) {
         return result;
      }
      for(Map.Entry<String, Set<String>> entry : instances.entrySet()) {
         if(entry.getValue() != null && entry.getValue().contains(SwitchCapability.NAMESPACE)) {
            result.add(entry.getKey());
         }
      }
      return result;
   }
}
