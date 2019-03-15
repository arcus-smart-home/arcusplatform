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
public class AttributeNotEmptyPredicate implements Predicate<Model>, Serializable {
   private final String attributeName;

   public AttributeNotEmptyPredicate(String attributeName) {
      Preconditions.checkArgument(!StringUtils.isEmpty(attributeName), "attributeName may not be empty");
      this.attributeName = attributeName;
   }
   
   @Override
   public String toString() {
      return attributeName + " is not empty";
   }

   @Override
   public boolean apply(Model model) {
      if(model == null) {
         return false;
      }
      
      Object value = model.getAttribute(attributeName);
      if(value == null) {
      	return false;
      }
      if(value instanceof String) {
      	return !((String) value).isEmpty();
      }
      if(value instanceof Collection) {
      	return !((Collection<?>) value).isEmpty();
      }
      if(value instanceof Map) {
      	return !((Map<?, ?>) value).isEmpty();
      }
      return true;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AttributeNotEmptyPredicate other = (AttributeNotEmptyPredicate) obj;
      if (attributeName == null) {
         if (other.attributeName != null) return false;
      }
      else if (!attributeName.equals(other.attributeName)) return false;
      return true;
   }
   

}

