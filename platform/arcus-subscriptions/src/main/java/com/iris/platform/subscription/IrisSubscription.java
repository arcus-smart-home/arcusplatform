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
package com.iris.platform.subscription;

import com.iris.messages.model.ServiceAddon;
import com.iris.messages.model.ServiceLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Model of a subscription in Iris with no Recurly information.
public class IrisSubscription {
   private final Set<ServiceLevel> serviceSet;
   private final Map<ServiceLevel, Integer> serviceLevels;
   private final Map<ServiceLevel, Map<String, Integer>> addons;

   private IrisSubscription(Map<ServiceLevel, Integer> serviceLevels, Map<ServiceLevel, Map<String, Integer>> addons) {
      this.serviceLevels = Collections.unmodifiableMap(serviceLevels);
      Map<ServiceLevel, Map<String, Integer>> tempAddonsMap = new HashMap<>();
      for (ServiceLevel addonKey : addons.keySet()) {
         Map<String, Integer> addonMap = addons.get(addonKey);
         if (addonMap != null) {
            tempAddonsMap.put(addonKey, Collections.unmodifiableMap(addonMap));
         }
      }
      this.addons = Collections.unmodifiableMap(tempAddonsMap);
      Set<ServiceLevel> serviceSet = new HashSet<>(this.serviceLevels.keySet());
      serviceSet.addAll(this.addons.keySet());
      this.serviceSet = Collections.unmodifiableSet(serviceSet);
   }
   
   public Set<ServiceLevel> getServiceLevels() {
      return this.serviceSet;
   }
   
   public Integer getNumberOfSubscriptions(ServiceLevel serviceLevel) {
      return serviceLevels.get(serviceLevel);
   }
   
   public Map<ServiceLevel, Integer> getAllSubscriptions() {
      return serviceLevels;
   }
   
   public Map<String, Integer> getNumberOfAddons(ServiceLevel serviceLevel) {
      return addons.get(serviceLevel);
   }
   
   public Map<ServiceLevel, Map<String, Integer>> getAllAddons() {
      return addons;
   }
   
   @Override
   public String toString() {
      return "IrisSubscription [serviceSet=" + serviceSet + ", serviceLevels="
            + serviceLevels + ", addons=" + addons + "]";
   }

   public static Builder builder() {
      return new Builder();
   }
   
   public static class Builder {
      private Map<ServiceLevel, Integer> serviceLevels = new HashMap<>();
      private Map<ServiceLevel, Map<String, Integer>> addons = new HashMap<>();
      
      
      private Builder() {}
      
      public Builder setInitialSubscription(IrisSubscription subscription) {
         this.serviceLevels.putAll(subscription.getAllSubscriptions());
         this.addons.putAll(subscription.getAllAddons());
         return this;
      }
      
      public Builder setNumberOfSubscriptions(ServiceLevel serviceLevel, int number) {
         this.serviceLevels.put(serviceLevel, number);
         return this;
      }
      
      public Builder addSubscription(ServiceLevel serviceLevel) {
         Integer quantity = serviceLevels.get(serviceLevel);
         serviceLevels.put(serviceLevel, quantity != null ? quantity + 1 : 1);
         return this;
      }
      
      public Builder addAddon(ServiceLevel serviceLevel, Map<String, Boolean> addons) {
         if (addons != null) {
            Map<String, Integer> currentAddons = this.addons.get(serviceLevel);
            Map<String, Integer> addonMap = currentAddons != null ? new HashMap<>(currentAddons) : new HashMap<>();
            for (String addonKey : addons.keySet()) {
               if (addons.get(addonKey) != null && addons.get(addonKey)) {
                  Integer addonCount = addonMap.get(addonKey);
                  addonMap.put(ServiceAddon.getAddonCode(ServiceAddon.fromString(addonKey), serviceLevel), (addonCount != null) ? addonCount + 1 : 1);
               }
            }
            this.addons.put(serviceLevel, addonMap);
         }
         return this;
      }
      
      public Builder setAddons(ServiceLevel serviceLevel, Map<String, Integer> addons) {
    	  Map<String, Integer> updatedAddons = new HashMap<String, Integer>();
    	  addons.forEach((k, v) -> {
    		  updatedAddons.put(ServiceAddon.getAddonCode(ServiceAddon.fromString(k), serviceLevel), v);
    	  });
         this.addons.put(serviceLevel, updatedAddons);
         return this;
      }
      
      public IrisSubscription create() {
         return new IrisSubscription(serviceLevels, addons);
      }
   }
}

