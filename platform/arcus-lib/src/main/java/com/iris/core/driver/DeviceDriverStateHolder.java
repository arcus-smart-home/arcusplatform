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
package com.iris.core.driver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iris.device.attributes.AttributeMap;

/**
 * holder for the attributes and variables for a context for passing back and forth to the DAO
 */
public class DeviceDriverStateHolder {
   private final AttributeMap attributes;
   private final Map<String,Object> variables;

   public DeviceDriverStateHolder() {
      this(null, null);
   }

   public DeviceDriverStateHolder(AttributeMap attributes) {
      this(attributes, null);
   }

   public DeviceDriverStateHolder(AttributeMap attributes, Map<String,Object> variables) {
      this.attributes = attributes == null ? AttributeMap.newMap() : AttributeMap.copyOf(attributes);
      this.variables = Collections.unmodifiableMap(variables == null ? new HashMap<String,Object>() : variables);
   }

   public AttributeMap getAttributes() {
      return attributes;
   }

   public Map<String,Object> getVariables() {
      return Collections.unmodifiableMap(variables);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "DeviceDriverStateHolder [attributes=" + attributes
            + ", variables=" + variables + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attributes == null) ? 0 : attributes.hashCode());
      result = prime * result
            + ((variables == null) ? 0 : variables.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      DeviceDriverStateHolder other = (DeviceDriverStateHolder) obj;
      if (attributes == null) {
         if (other.attributes != null)
            return false;
      } else if (!attributes.equals(other.attributes))
         return false;
      if (variables == null) {
         if (other.variables != null)
            return false;
      } else if (!variables.equals(other.variables))
         return false;
      return true;
   }
}

