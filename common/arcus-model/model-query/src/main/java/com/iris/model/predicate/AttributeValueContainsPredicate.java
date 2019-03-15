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
package com.iris.model.predicate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.messages.model.Model;

/**
 * 
 */
public class AttributeValueContainsPredicate implements Predicate<Model>, Serializable {
   private final String attributeName;
   private final Object containedValue;

   public AttributeValueContainsPredicate(String attributeName, Object containedValue) {
      Preconditions.checkArgument(!StringUtils.isEmpty(attributeName), "attributeName may not be empty");
      Preconditions.checkNotNull(containedValue, "containedValue may not be null");
      this.attributeName = attributeName;
      this.containedValue = containedValue;
   }

   @Override
   public boolean apply(Model model) {
      if(model == null) {
         return false;
      }
      
      Object value = model.getAttribute(attributeName);
      if(value == null || !(value instanceof Map)) {
         // TODO logger
         return false;
      }
      
      for(Object v: ((Map<?, ?>) value).values()) {
      	if(v != null && v instanceof Collection<?> && ((Collection<?>) v).contains(containedValue)) {
      		return true;
      	}
      }
      
      // TODO logger
      return false;
   }

   @Override
   public String toString() {
      return attributeName + " has a value containing " + containedValue;
   }
   
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attributeName == null) ? 0 : attributeName.hashCode());
      result = prime * result
            + ((containedValue == null) ? 0 : containedValue.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AttributeValueContainsPredicate other = (AttributeValueContainsPredicate) obj;
      if (attributeName == null) {
         if (other.attributeName != null) return false;
      }
      else if (!attributeName.equals(other.attributeName)) return false;
      if (containedValue == null) {
         if (other.containedValue != null) return false;
      }
      else if (!containedValue.equals(other.containedValue)) return false;
      return true;
   }
   

}

