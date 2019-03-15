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
package com.iris.io.json.gson;

import java.lang.reflect.Type;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.iris.type.LooselyTypedReference;
import com.iris.util.TypeMarker;

/**
 * 
 */
class GsonReference implements LooselyTypedReference {
   private static final Logger logger = LoggerFactory.getLogger(GsonReference.class);
   
   public static LooselyTypedReference wrap(Gson gson, JsonElement element) {
      if(element == null || element.isJsonNull()) {
         return NULL_REFERENCE;
      }
      return new GsonReference(gson, element);
   }
   
   private static LooselyTypedReference NULL_REFERENCE = new LooselyTypedReference() {
      
      @Override
      public boolean isNull() {
         return true;
      }
      
      @Override
      @Nullable
      public <T> T as(TypeMarker<T> type, @Nullable T defaultValue) {
         return defaultValue;
      }
      
      @Override
      @Nullable
      public <T> T as(Class<T> type, @Nullable T defaultValue) {
         return defaultValue;
      }
      
      @Override
      @Nullable
      public <T> T as(TypeMarker<T> type) {
         return null;
      }
      
      @Override
      @Nullable
      public <T> T as(Class<T> type) {
         return null;
      }
      
      @Override
      public String toString() {
         return "JsonReference [null]";
      }

   };
   
   private Gson gson;
   private JsonElement element;

   private GsonReference(Gson gson, JsonElement element) {
      this.gson = gson;
      this.element = element;
   }
   
   /* (non-Javadoc)
    * @see com.iris.type.LooselyTypedReference#isNull()
    */
   @Override
   public boolean isNull() {
      return false;
   }

   /* (non-Javadoc)
    * @see com.iris.type.LooselyTypedReference#as(java.lang.Class)
    */
   @Override
   @Nullable
   public <T> T as(Class<T> type) {
      return fromJson(type, null);
   }

   /* (non-Javadoc)
    * @see com.iris.type.LooselyTypedReference#as(com.iris.util.TypeMarker)
    */
   @Override
   @Nullable
   public <T> T as(TypeMarker<T> type) {
      return fromJson(type.getType(), null);
   }

   /* (non-Javadoc)
    * @see com.iris.type.LooselyTypedReference#as(java.lang.Class, java.lang.Object)
    */
   @Override
   @Nullable
   public <T> T as(Class<T> type, @Nullable T defaultValue) {
      return fromJson(type, defaultValue);
   }

   /* (non-Javadoc)
    * @see com.iris.type.LooselyTypedReference#as(com.iris.util.TypeMarker, java.lang.Object)
    */
   @Override
   @Nullable
   public <T> T as(TypeMarker<T> type, @Nullable T defaultValue) {
      return fromJson(type.getType(), defaultValue);
   }

   @SuppressWarnings("unchecked")
   protected <T> T fromJson(Type type, @Nullable T defaultValue) {
      Object value = null;
      try {
         value = gson.fromJson(element, type);
      }
      catch(Exception e) {
         logger.debug("Can't coerce element to [{}]", type, e);
      }
      return value == null ? defaultValue : (T) value;
   }

   @Override
   public String toString() {
      return "JsonReference [" + element + "]";
   }
}

