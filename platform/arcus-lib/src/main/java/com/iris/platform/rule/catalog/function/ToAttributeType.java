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
package com.iris.platform.rule.catalog.function;

import com.google.common.base.Function;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;

public final class ToAttributeType implements Function<String, AttributeType> {
   private static final ToAttributeType INSTANCE = new ToAttributeType();
   
   public static ToAttributeType instance() {
      return INSTANCE;
   }
   
   private ToAttributeType() {
      
   }

   /* (non-Javadoc)
    * @see com.google.common.base.Function#apply(java.lang.Object)
    */
   @Override
   public AttributeType apply(String input) {
      return AttributeTypes.parse(input);
   }

   @Override
   public String toString() {
      return "ToAttributeType";
   }

   @Override
   public int hashCode() {
      return 42;
   }
   
   @Override
   public boolean equals(Object o) {
      return o != null && o instanceof ToAttributeType;
   }
}


