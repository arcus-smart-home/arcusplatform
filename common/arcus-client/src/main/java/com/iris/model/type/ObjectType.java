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
package com.iris.model.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Represents a restriction on {@link MapType}. Each
 * field has a specific attribute type it may contain.
 */
public class ObjectType implements AttributeType {
   // TODO move this out..
   private static final Gson JSON = new GsonBuilder().create();

   private final Map<String, AttributeType> fields = new HashMap<>();
   
   /* (non-Javadoc)
    * @see com.iris.model.type.AttributeType#getJavaType()
    */
   @Override
   public Class<?> getJavaType() {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see com.iris.model.type.AttributeType#getTypeName()
    */
   @Override
   public String getTypeName() {
      // TODO allow a type name to be specified?
      return "object";
   }

   /* (non-Javadoc)
    * @see com.iris.model.type.AttributeType#coerce(java.lang.Object)
    */
   @Override
   public Object coerce(Object obj) {
      if(obj == null) {
         return null;
      }

      // TODO allow direct pass-through of map?
      JsonElement elem = JSON.toJsonTree(obj);
      if(!elem.isJsonObject()) {
         throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + getTypeName());
      }
      
      return coerce(null, elem.getAsJsonObject());
   }

   protected Object coerce(String fieldName, JsonObject jsonObject) {
      Map<String, Object> result = new HashMap<String, Object>(fields.size() + 1);
      for(Map.Entry<String, JsonElement> entry: jsonObject.entrySet()) {
         String key = entry.getKey();
         AttributeType fieldType = fields.get(key);
         if(fieldType == null) {
            // TODO allow unrecognized fields through?
            continue;
         }
         
         JsonElement value = entry.getValue();
         if(value.isJsonPrimitive()) {
            // always treat primitives as strings and let the AttributeType system hash it out
            result.put(key, fieldType.coerce(value.getAsString()));
         }
         else if(value.isJsonArray()) {
            result.put(key, coerceCollection(fieldName != null ? fieldName + "." + key : key, value.getAsJsonArray(), fieldType));
         }
         else if(value.isJsonObject()) {
            result.put(key, coerceObject(fieldName != null ? fieldName + "." + key : key, value.getAsJsonObject(), fieldType));
         }
         else {
            // TODO just drop it?
            throw new IllegalArgumentException("Cannot coerce field " + fieldName + " to " + fieldType.getTypeName());
         }
      }
      return result;
   }
   
   private Object coerceElement(String fieldName, JsonElement value, AttributeType fieldType) {
      if(value.isJsonPrimitive()) {
         // always treat primitives as strings and let the AttributeType system hash it out
         try {
            return fieldType.coerce(value.getAsString());
         }
         catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot coerce field " + fieldName + " to " + fieldType.getTypeName(), e);
         }
      }
      else if(value.isJsonArray()) {
         return coerceCollection(fieldName, value.getAsJsonArray(), fieldType);
      }
      else if(value.isJsonObject()) {
         return coerceObject(fieldName, value.getAsJsonObject(), fieldType);
      }
      else {
         // TODO just drop it?
         throw new IllegalArgumentException("Cannot coerce field " + fieldName + " to " + fieldType.getTypeName());
      }
   }
   
   private Object coerceCollection(String fieldName, JsonArray jsonArray, AttributeType fieldType) {
      Collection<Object> collection;
      AttributeType containedType;
      if(fieldType instanceof SetType) {
         if(jsonArray.size() == 0) {
            return new HashSet<Object>();
         }
         collection = new HashSet<Object>(jsonArray.size());
         containedType = ((SetType) fieldType).getContainedType();
      }
      else if(fieldType instanceof ListType || fieldType instanceof AnyType) {
         if(jsonArray.size() == 0) {
            return new ArrayList<Object>();
         }
         collection = new ArrayList<Object>(jsonArray.size());
         containedType = (fieldType instanceof AnyType) ? AnyType.INSTANCE : ((ListType) fieldType).getContainedType();
      }
      else {
         // TODO just drop it?
         throw new IllegalArgumentException("Cannot coerce field " + fieldName + " to " + fieldType.getTypeName());
      }
      
      int index = 0;
      for(JsonElement e: jsonArray) {
         collection.add(coerceElement(fieldName + "[" + index + "]", e, containedType));
      }
      return collection;
   }

   private Object coerceObject(String fieldName, JsonObject jsonObject, AttributeType fieldType) {
      if(fieldType instanceof ObjectType) {
         return ((ObjectType) fieldType).coerce(fieldName, jsonObject);
      }
      else if(fieldType instanceof MapType || fieldType instanceof AnyType) {
         Map<String, Object> result = new HashMap<String, Object>();
         AttributeType containedType = (fieldType instanceof AnyType) ? AnyType.INSTANCE : ((MapType) fieldType).getContainedType();
         for(Map.Entry<String, JsonElement> entry: jsonObject.entrySet()) {
            Object value = coerceElement(fieldName + "." + entry.getKey(), entry.getValue(), containedType);
            result.put(entry.getKey(), value);
         }
         return result;
      }
      else {
         throw new IllegalArgumentException("Cannot coerce field " + fieldName + " to " + fieldType.getTypeName());
      }
   }

   /* (non-Javadoc)
    * @see com.iris.model.type.AttributeType#isAssignableFrom(java.lang.reflect.Type)
    */
   @Override
   public boolean isAssignableFrom(Type type) {
      if(type instanceof Class) {
         Class<?> cls = (Class<?>) type;
         return
               !Boolean.class.isAssignableFrom(cls) &&
               !Number.class.isAssignableFrom(cls) &&
               !String.class.isAssignableFrom(cls) &&
               !Date.class.isAssignableFrom(cls) &&
               !Iterable.class.isAssignableFrom(cls);
      }
      if(type instanceof ParameterizedType) {
         ParameterizedType pType = (ParameterizedType) type;
         if(Map.class.isAssignableFrom((Class<?>) pType.getRawType())) {
            return 
                  pType.getActualTypeArguments().length == 2 &&
                  String.class.equals(pType.getActualTypeArguments()[0]);
         }
         else {
            return isAssignableFrom(pType.getRawType());
         }
      }
      return false;
   }

}

