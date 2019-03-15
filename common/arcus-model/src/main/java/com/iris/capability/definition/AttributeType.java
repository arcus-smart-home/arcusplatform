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
package com.iris.capability.definition;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

/**
 * 
 */
public interface AttributeType {

   public RawType getRawType();
   
   public Type getJavaType();
   
   public String getRepresentation();
   
   public boolean isPrimitive();
   
   public boolean isEnum();
   
   public boolean isCollection();
   
   public boolean isObject();
   
   @Nullable
   public EnumType asEnum();
   
   @Nullable
   public CollectionType asCollection();
   
   @Nullable
   public ObjectType asObject();
   
   @Nullable
   public Object coerce(@Nullable Object value);
   
   public enum RawType {
      VOID(Object.class),
      BOOLEAN(Boolean.class),
      BYTE(Byte.class),
      INT(Integer.class),
      LONG(Long.class),
      TIMESTAMP(Date.class),
      DOUBLE(Double.class),
      STRING(String.class),
      ENUM(Enum.class),
      SET(Set.class),
      LIST(List.class),
      MAP(Map.class),
      ATTRIBUTES(Map.class),
      OBJECT(Object.class), // ???
      ANY(Object.class);
      
      private Class<?> javaType;
      
      RawType(Class<?> javatype) {
         this.javaType = javatype;
      }
      
      public Class<?> getJavaType() {
         return javaType;
      }
      
   }
   
   public static interface EnumType extends AttributeType {
      
      public Set<String> getValues();
      
      /**
       * The enumeration of the EnumType
       * 
       * @param obj - An object whose toString operator matches one of this EnumType's values.
       * @return - the index in the array of values, ordered by insertion.
       */
      public int ordinal(Object obj);  // the enumeration of the EnumType
   }
   
   public static interface CollectionType extends AttributeType {
      
      public AttributeType getContainedType();
   }
   
   public static interface ObjectType extends AttributeType {
      
      public Map<String, AttributeType> getAttributes();
   }
}

