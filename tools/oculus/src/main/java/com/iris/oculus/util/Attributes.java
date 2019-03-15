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
package com.iris.oculus.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeType.EnumType;
import com.iris.io.json.JSON;
import com.iris.util.TypeMarker;

/**
 * 
 */
public class Attributes {

   public static String render(AttributeType type, Object value) {
      return render(type, value, "");
   }
   
   public static String render(AttributeType type, Object value, String defaultValue) {
      // might get smarter with type in the future
      if(value == null) {
         return defaultValue;
      }
      if(type.isPrimitive() || type.isEnum()) {
         return value.toString();
      }
      return JSON.toJson(value);
   }
   
   public static Function<Object, String> renderer(AttributeType type) {
      return renderer(type, "");
   }

   public static Function<Object, String> renderer(AttributeType type, String defaultValue) {
      return (o) -> render(type, o, defaultValue);
   }

   public static Object parse(AttributeType type, String value) {
      if(StringUtils.isEmpty(value)) {
         return null;
      }
      switch(type.getRawType()) {
      case BOOLEAN:
         return Boolean.parseBoolean(value);
      case BYTE:
         return Byte.parseByte(value);
      case INT:
         return Integer.parseInt(value);
      case LONG:
         return Long.parseLong(value);
      case DOUBLE:
         return Double.parseDouble(value);
      case TIMESTAMP:
         return new Date(Long.parseLong(value));
      case ENUM:
         return desensitize(type.asEnum(), value);
      case STRING:
         return value;
      default:
         Type t = type.getJavaType();
         return JSON.fromJson(value, TypeMarker.wrap(t));
      }
   }

   private static Object desensitize(EnumType e, String value) {
      for(String v: e.getValues()) {
         if(v.equalsIgnoreCase(value)) {
            return v;
         }
      }
      throw new IllegalArgumentException("Invalid enum value [" + value + "] must be one of " + e.getValues());
   }

   public static Function<String, Object> parser(AttributeType type) {
      return (text) -> parse(type, text);
   }
   
}

