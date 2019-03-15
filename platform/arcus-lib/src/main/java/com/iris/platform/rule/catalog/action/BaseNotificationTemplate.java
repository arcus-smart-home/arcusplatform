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
package com.iris.platform.rule.catalog.action;

import java.util.Map;
import java.util.Set;

import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.platform.rule.catalog.template.TemplatedValue;

public abstract class BaseNotificationTemplate extends AbstractActionTemplate{
   
   public BaseNotificationTemplate(Set<String> contextVariables) {
      super(contextVariables);
   }
   
   protected Address getToAddress(TemplatedValue<String> to,Map<String, Object> variables){
      Address address = Address.fromString(to.apply(variables));
      if(!PersonCapability.NAMESPACE.equals(address.getGroup())) {
         throw new IllegalArgumentException("to must be a person address");
      }
      if(address.getId() == null) {
         throw new IllegalArgumentException("to must specify a person id");
      }
      return address;
   }
   
}

