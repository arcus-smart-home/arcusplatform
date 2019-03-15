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
package com.iris.platform.hubbridge;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;

public class HeartbeatMessage {
   public static final String NAME = "hubbridge:Heartbeat";
   public static final String ATTR_CONNECTED_HUB_IDS = "connectedHubIds";
   public static final AttributeType TYPE_CONNECTED_HUB_IDS = AttributeTypes.setOf(AttributeTypes.stringType());
   
   public static Set<String> getConnectedHubIds(Map<String, Object> attributes) {
      return (Set<String>) TYPE_CONNECTED_HUB_IDS.coerce(attributes.get(ATTR_CONNECTED_HUB_IDS));
   }
   
   public static Builder builder() {
      return new Builder();
   }
   
   public static Builder builder(com.iris.messages.MessageBody body) {
      return new Builder(body);
   }
   
   public static class Builder {
      
      private Set<String> connectedHubIds = ImmutableSet.of();
      

      Builder() {
      }

      Builder(com.iris.messages.MessageBody body) {
          this(body != null ? body.getAttributes() : null);
      }

      Builder(java.util.Map<String, Object> attributes) {
         if(attributes != null) {
           this.connectedHubIds = HeartbeatMessage.getConnectedHubIds(attributes);
         }
      }

      
      public Set<String> getConnectedHubIds() {
         return connectedHubIds;
      }

      public Builder withConnectedHubIds(Set<String> value) {
         this.connectedHubIds = value;
         return this;
      }
      
      public com.iris.messages.MessageBody build() {
         return com.iris.messages.MessageBody.buildMessage(NAME, ImmutableMap.of(ATTR_CONNECTED_HUB_IDS, connectedHubIds));
      }
   }
}

