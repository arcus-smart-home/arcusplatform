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
package com.iris.driver.reflex;

public final class ReflexActionSetAttribute implements ReflexAction {
   private final String attr;
   private final Object value;

   public ReflexActionSetAttribute(String attr, Object value) {
      this.attr = attr;
      this.value = value;
   }

   public String getAttr() {
      return attr;
   }

   public Object getValue() {
      return value;
   }

   @Override
   public String toString() {
      return "ReflexActionSetAttribute [" +
         "attr=" + attr + 
         ",value=" + value +
         "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((attr == null) ? 0 : attr.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
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
      ReflexActionSetAttribute other = (ReflexActionSetAttribute) obj;
      if (attr == null) {
         if (other.attr != null)
            return false;
      } else if (!attr.equals(other.attr))
         return false;
      if (value == null) {
         if (other.value != null)
            return false;
      } else if (!value.equals(other.value))
         return false;
      return true;
   }
}

