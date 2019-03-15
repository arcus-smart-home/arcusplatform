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
package com.iris.platform.rule.catalog.function;

import com.google.common.base.Function;
import com.iris.capability.definition.AttributeType;

/**
 * 
 */
public class CoerceFunction implements Function<Object, Object> {
   private final AttributeType type;
   
   public CoerceFunction(AttributeType type) {
      this.type = type;
   }

   /* (non-Javadoc)
    * @see com.google.common.base.Function#apply(java.lang.Object)
    */
   @Override
   public Object apply(Object input) {
      return type.coerce(input);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "CoerceFunction [type=" + type + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      CoerceFunction other = (CoerceFunction) obj;
      if (type == null) {
         if (other.type != null) return false;
      }
      else if (!type.equals(other.type)) return false;
      return true;
   }

}

