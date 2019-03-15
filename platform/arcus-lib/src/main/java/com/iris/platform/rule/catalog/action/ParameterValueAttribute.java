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

import com.google.common.base.Function;
import com.iris.common.rule.Context;
import com.iris.messages.address.Address;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedValue;

public class ParameterValueAttribute implements ParameterValue {
   
   private String attribute;
   private TemplatedValue<Address> address;

   @Override
   public Function<Context, String> getValueFunction(Map<String, Object> variables) {
      Address resolvedAddress = address.apply(variables);
      if (resolvedAddress == null) {
         throw new IllegalArgumentException("object must be a resolveable address");
      }
      return FunctionFactory.INSTANCE.createGetAttribute(String.class, resolvedAddress, attribute);
   }

   public String getAttribute() {
      return attribute;
   }

   public void setAttribute(String attribute) {
      this.attribute = attribute;
   }

   public TemplatedValue<Address> getAddress() {
      return address;
   }

   public void setAddress(TemplatedValue<Address> address) {
      this.address = address;
   }
}

