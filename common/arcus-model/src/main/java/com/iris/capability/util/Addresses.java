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
package com.iris.capability.util;

import org.eclipse.jdt.annotation.Nullable;

public class Addresses {
   private static final String SERVICE_ADDRESS_PREFIX = "SERV:";
   private static final String DRIVER_ADDRESS_PREFIX = "DRIV:";
   private static final String DRIVER_NAMESPACE = "dev";
   
   public static String toServiceAddress(String namespace) {
      if(namespace == null) throw new IllegalArgumentException("namespace may not be null");
      String prefix = getPrefixForNamespace(namespace);
      return prefix + namespace + ":";
   }
   
   public static String toObjectAddress(String namespace, String id) {
      if(id == null) {
         return toServiceAddress(namespace);
      }
      if(namespace == null) throw new IllegalArgumentException("namespace may not be null");
      String prefix = getPrefixForNamespace(namespace);
      return prefix + namespace + ":" + id;
   }
   
   public static String getId(@Nullable String addressOrId) {
      if(addressOrId == null) {
         return "";
      }
      int indexOf = addressOrId.lastIndexOf(":");
      if(indexOf < 0) {
         return addressOrId;
      }
      else if(indexOf == addressOrId.length() - 1) {
         return "";
      }
      return addressOrId.substring(indexOf + 1);
   }

   private static String getPrefixForNamespace(String namespace) {
      if(DRIVER_NAMESPACE.equals(namespace)) {
         return DRIVER_ADDRESS_PREFIX;
      }
      else {
         return SERVICE_ADDRESS_PREFIX;
      }
   }
}

