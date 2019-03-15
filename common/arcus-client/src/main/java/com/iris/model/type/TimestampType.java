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
package com.iris.model.type;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.reflect.TypeUtils;

public enum TimestampType implements PrimitiveType {
   INSTANCE;

   @Override
   public String getTypeName() {
      return "timestamp";
   }

   @Override
   public Class<Date> getJavaType() {
      return Date.class;
   }

   @Override
   public Date coerce(Object obj) {
      if(obj == null) {
         return null;
      }

      if(obj instanceof Date) {
         return (Date) obj;
      }

      if(obj instanceof Calendar) {
         return ((Calendar) obj).getTime();
      }

      if(obj instanceof Number) {
         return new Date(((Number)obj).longValue());
      }

      if(obj instanceof String) {
         // TODO:  we could support parsing strings as well, but it is unclear what datetime format
         // we can expect
         try {
            Long millis = Long.parseLong((String) obj);
            return new Date(millis);
         } catch(NumberFormatException nfe) {
            throw new IllegalArgumentException("Cannot coerce " + obj + " to " + getTypeName());
         }
      }

      throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + getTypeName());
   }

   @Override
   public boolean isAssignableFrom(Type type) {
      if(type == null) {
         return false;
      }
      return 
            Date.class.equals(type) ||
            Long.class.equals(type) || 
            Calendar.class.equals(type) ||
            Number.class.isAssignableFrom(TypeUtils.getRawType(type, null));
   }

   @Override
   public String toString() {
      return "timestamp";
   }

}

