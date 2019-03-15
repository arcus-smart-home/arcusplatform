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

import org.apache.commons.lang3.reflect.TypeUtils;

public enum BooleanType implements PrimitiveType {
	INSTANCE;

	@Override
	public String getTypeName() {
		return "boolean";
	}

	@Override
	public Class<Boolean> getJavaType() {
		return Boolean.class;
	}

   @Override
   public Boolean coerce(Object obj) {
      if(obj == null) {
         return null;
      }

      if(obj instanceof Boolean) {
         return (Boolean) obj;
      }

      if(obj instanceof Number) {
         // TODO:  should we only allow 0 or 1?
         return ((Number) obj).longValue() != 0;
      }

      if(obj instanceof String) {
         // TODO:  should we coerce text such as "yes", "1"?
         return Boolean.valueOf((String) obj);
      }

      throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + getTypeName());
   }

   @Override
   public boolean isAssignableFrom(Type type) {
      if(type == null) {
         return false;
      }
      return 
            Boolean.class.equals(type) || 
            String.class.equals(type) || 
            Number.class.isAssignableFrom(TypeUtils.getRawType(type, null));
   }

   @Override
   public String toString() {
      return "boolean";
   }
   
}

