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
package com.iris.model.predicate;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.iris.messages.model.Model;

/**
 * Matches models that have instanced attributes for a given capability namespace.
 * Unlike AttributeSupportedPredicate, this does NOT match models that only have
 * the base (non-instanced) attribute.
 */
public class InstancedAttributeSupportedPredicate implements Predicate<Model>, Serializable {
   private final String capabilityNamespace;

   public InstancedAttributeSupportedPredicate(String capabilityNamespace) {
      this.capabilityNamespace = capabilityNamespace;
   }

   @Override
   public boolean apply(Model model) {
      if (model == null) {
         return false;
      }
      Map<String, Set<String>> instances = model.getInstances();
      if (instances == null || instances.isEmpty()) {
         return false;
      }
      for (Set<String> caps : instances.values()) {
         if (caps != null && caps.contains(capabilityNamespace)) {
            return true;
         }
      }
      return false;
   }
}
