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
package com.iris.validators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 *
 */
public class Validator {
   private String message;
   private List<String> errors;

   public Validator() {
      this("Validation failed, details:");
   }
   
   public Validator(String message) {
      this.message = message;
      this.errors = new ArrayList<>();
   }
   
   public Validator(Validator toCopy) {
      this.message = toCopy.message;
      this.errors = new ArrayList<>(toCopy.errors);
   }
   
   public String getMessage() {
      return message;
   }
   
   public void setMessage(String message) {
      this.message = message;
   }
   
   public boolean assertNotEmpty(String v, String message) {
      return assertFalse(StringUtils.isEmpty(v), message);
   }
   
   public boolean assertNotEmpty(Collection<?> c, String message) {
      return assertFalse(c == null || c.isEmpty(), message);
   }
   
   public boolean assertNotEmpty(Map<?, ?> m, String message) {
      return assertFalse(m == null || m.isEmpty(), message);
   }
   
   public boolean assertNotNull(Object o, String message) {
      return assertTrue(o != null, message);
   }
   
   public boolean assertTrue(boolean value, String message) {
      if(!value) {
         error(message);
      }
      return value;
   }
   
   public boolean assertFalse(boolean value, String message) {
      return assertTrue(!value, message);
   }
   
   public Validator error(String message) {
      errors.add(message);
      return this;
   }
   
   public void addErrors(Validator validator) {
      if(validator == null) {
         return;
      }
      this.errors.addAll(validator.errors);
   }

   public boolean hasErrors() {
      return !errors.isEmpty();
   }
   
   public void throwIfErrors() throws ValidationException {
      if(errors.isEmpty()) {
         return;
      }
      if(errors.size() == 1) {
         throw new ValidationException(errors.get(0));
      }
      else {
         StringBuilder sb = new StringBuilder(message).append("\n");
         int i=0;
         for(String error: errors) {
            i++;
            sb.append("\n  ")
               .append(i)
               .append(") ")
               .append(error.replace("\n", "\n    "));
         }
         sb.append("\n");
         throw new ValidationException(sb.toString());
      }
   }

   @Override
   public String toString() {
      return "Validator [" + (hasErrors() ? "errors=" + errors : "Valid") + "]";
   }

}

