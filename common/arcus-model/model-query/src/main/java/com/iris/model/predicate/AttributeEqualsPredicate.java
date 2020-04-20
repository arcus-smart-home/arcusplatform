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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.messages.model.Model;

/**
 * 
 */
public class AttributeEqualsPredicate implements Predicate<Model>, Serializable {
   private static final Logger logger = LoggerFactory.getLogger(AttributeEqualsPredicate.class);

   private final String attributeName;
   private final Object attributeValue;

   public AttributeEqualsPredicate(String attributeName, Object attributeValue) {
      Preconditions.checkArgument(!StringUtils.isEmpty(attributeName), "attributeName may not be empty");
      this.attributeName = attributeName;
      this.attributeValue = attributeValue;
   }

   @Override
   public boolean apply(Model model) {
      if(model == null) {
         return false;
      }
      
      Object value = model.getAttribute(attributeName);
      if(value == null) {
         // TODO logger
         return false;
      }

      if(!Objects.equal(attributeValue, value)) {
         logger.trace("comparison for [attributeName={}, attributeValue={}, value={}] is not equal",
                 attributeName, attributeValue, value);
         return false;
      }

      logger.trace("comparison for [attributeName={}, attributeValue={}, value={}] is equal",
              attributeName, attributeValue, value);
      return true;
   }

   @Override
   public String toString() {
      return attributeName + " equals " + attributeValue;
   }
   
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attributeName == null) ? 0 : attributeName.hashCode());
      result = prime * result
            + ((attributeValue == null) ? 0 : attributeValue.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AttributeEqualsPredicate other = (AttributeEqualsPredicate) obj;
      if (attributeName == null) {
         if (other.attributeName != null) return false;
      }
      else if (!attributeName.equals(other.attributeName)) return false;
      if (attributeValue == null) {
         if (other.attributeValue != null) return false;
      }
      else if (!attributeValue.equals(other.attributeValue)) return false;
      return true;
   }
   

}

