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
package com.iris.protocol.zigbee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.messages.MessageBody;

public class ZigbeeBindEvent {
   public static final String NAME = "ZigbeeBindEvent";

   public static final String ATTR_ENDPOINT_BINDINGS = "endpointBindings";
   public static final String ATTR_PROFILE_BINDINGS = "profileBindings";

   public static MessageBody createAllBindings() {
      return MessageBody.messageBuilder(NAME).create();
   }

   public static MessageBody createProfileBindings(List<Integer> bindings) {
      Map<String,Object> attrs = ImmutableMap.<String,Object>of(
         ATTR_PROFILE_BINDINGS, ImmutableList.copyOf(bindings)
      );

      return MessageBody.messageBuilder(NAME).withAttributes(attrs).create();
   }

   public static MessageBody createEndpointBindings(Map<Integer,List<Binding>> bindings) {
      Map<String,Object> attrs = ImmutableMap.<String,Object>of(
         ATTR_ENDPOINT_BINDINGS, bindings
      );

      return MessageBody.messageBuilder(NAME).withAttributes(attrs).create();
   }

   @Nullable
   public static Map<Integer,List<Binding>> getEndpointBindings(MessageBody message) {
      if (message == null) {
         return null;
      }

      Object bindings = message.getAttributes().get(ATTR_ENDPOINT_BINDINGS);
      if (bindings == null) {
         return null;
      }

      Map<Integer,List<Binding>> result = new HashMap<Integer,List<Binding>>();
      Map<Object,List<Object>> mbindings = (Map<Object,List<Object>>)bindings;
      for (Map.Entry<Object,List<Object>> entry : mbindings.entrySet()) {
         Object key = entry.getKey();

         Integer ckey;
         if (key instanceof Number) {
            ckey = ((Number)key).intValue();
         } else if (key instanceof CharSequence) {
            ckey = Integer.parseInt(((CharSequence)key).toString());
         } else {
            throw new RuntimeException("unknown binding key value: " + key.getClass());
         }

         List<Binding> binds = new ArrayList<>();
         for (Object bentry : entry.getValue()) {
            if (bentry instanceof Binding) {
               binds.add((Binding)bentry);
            } else if (bentry instanceof Map) {
               Map m = (Map)bentry;

               int clusterId = ((Number)m.get("clusterId")).intValue();
               boolean client = ((Boolean)m.get("client")).booleanValue();
               binds.add(new Binding(clusterId, client));
            } else {
               throw new RuntimeException("unknown binding value: " + bentry.getClass());
            }
         }

         result.put(ckey, binds);
      }

      return result;
   }

   @Nullable
   public static List<Integer> getProfileBindings(MessageBody message) {
      if (message == null) {
         return null;
      }

      Object bindings = message.getAttributes().get(ATTR_PROFILE_BINDINGS);
      List<Number> nbindings = (List<Number>)bindings;
      if (nbindings == null) {
         return null;
      }

      List<Integer> results = new ArrayList<>();
      for (Number number : nbindings) {
         if (number != null) {
            results.add(number.intValue());
         }
      }

      return results;
   }

   public static final class Binding {
      private int clusterId;
      private boolean client;

      public Binding(int clusterId, boolean client) {
         this.clusterId = clusterId;
         this.client = client;
      }

      public int getClusterId() {
         return clusterId;
      }

      public void setClusterId(int clusterId) {
         this.clusterId = clusterId;
      }

      public boolean isClient() {
         return client;
      }

      public void setClient(boolean client) {
         this.client = client;
      }
   }
}

