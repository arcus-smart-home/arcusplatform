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
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.messages.model.Model;

/**
 * 
 */
public class AttributeLikePredicate implements Predicate<Model>, Serializable {
   private final String attributeName;
   private final Pattern attributePattern;

   public AttributeLikePredicate(String attributeName, String attributePattern) {
      this(attributeName, Pattern.compile(attributePattern));
   }
   
   public AttributeLikePredicate(String attributeName, Pattern attributePattern) {
      Preconditions.checkArgument(!StringUtils.isEmpty(attributeName), "attributeName may not be empty");
      Preconditions.checkNotNull(attributePattern, "attributePattern may not be null");
      
      this.attributeName = attributeName;
      this.attributePattern = attributePattern;
   }

   @Override
   public boolean apply(Model model) {
      Object value = model.getAttribute(attributeName);
      if(value == null || !(value instanceof CharSequence)) {
         // TODO logger
         return false;
      }
      
      if(!attributePattern.matcher((CharSequence) value).matches()) {
         // TODO logger
         return false;
      }
      
      // TODO logger
      return true;
   }

   @Override
   public String toString() {
      return attributeName + " is like " + attributePattern;
   }
   
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attributeName == null) ? 0 : attributeName.hashCode());
      result = prime * result
            + ((attributePattern == null) ? 0 : attributePattern.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AttributeLikePredicate other = (AttributeLikePredicate) obj;
      if (attributeName == null) {
         if (other.attributeName != null) return false;
      }
      else if (!attributeName.equals(other.attributeName)) return false;
      if (attributePattern == null) {
         if (other.attributePattern != null) return false;
      }
      else if (!attributePattern.equals(other.attributePattern)) return false;
      return true;
   }
   

}

