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
package com.iris.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.iris.messages.address.Address;

/**
 *
 */
// TODO TypeTypeAdapter should really be a Type Hiearchy adapter but we don't have any way to inject that information
public class AddressTypeAdapterFactory implements TypeAdapterFactory {
   private AddressTypeAdapter instance = new AddressTypeAdapter();
   
   /* (non-Javadoc)
    * @see com.google.gson.TypeAdapterFactory#create(com.google.gson.Gson, com.google.gson.reflect.TypeToken)
    */
   @Override
   public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if(Address.class.isAssignableFrom(type.getRawType())) {
         return (TypeAdapter<T>) instance;
      }
      return null;
   }

}

