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
package com.iris.platform.alarm.notification.calltree;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.messages.type.CallTreeEntry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CallTreeContext {

   private final Address incidentAddress;
   private final UUID placeId;
   private final String msgKey;
   private final Map<String, String> params;
   private final String priority;
   private final List<CallTreeEntry> callTree;
   private final long sequentialDelaySecs;

   private CallTreeContext(
      Address incidentAddress,
      UUID placeId,
      String msgKey,
      Map<String, String> params,
      String priority,
      List<CallTreeEntry> callTree,
      long sequentialDelaySecs
   ) {
      this.incidentAddress = incidentAddress;
      this.placeId = placeId;
      this.msgKey = msgKey;
      this.params = params;
      this.priority = priority;
      this.callTree = callTree;
      this.sequentialDelaySecs = sequentialDelaySecs;
   }

   public Address getIncidentAddress() {
      return incidentAddress;
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public String getMsgKey() {
      return msgKey;
   }

   public Map<String, String> getParams() {
      return params;
   }

   public String getPriority() {
      return priority;
   }

   public List<CallTreeEntry> getCallTree() {
      return callTree;
   }

   public long getSequentialDelaySecs() {
      return sequentialDelaySecs;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private Address incidentAddress;
      private UUID placeId;
      private String msgKey;
      private ImmutableMap.Builder<String, String> paramBuilder = ImmutableMap.builder();
      private String priority;
      private ImmutableList.Builder<CallTreeEntry> callTreeBuilder = ImmutableList.builder();
      private long sequentialDelaySecs = -1;

      private Builder() {
      }

      public Builder withIncidentAddress(Address incidentAddress) {
         this.incidentAddress = incidentAddress;
         return this;
      }

      public Builder withIncidentAddress(String incidentAddress) {
         Preconditions.checkNotNull(incidentAddress, "incidentAddress is required");
         return withIncidentAddress(Address.fromString(incidentAddress));
      }

      public Builder withPlaceId(UUID placeId) {
         this.placeId = placeId;
         return this;
      }

      public Builder withPlaceId(String placeId) {
         Preconditions.checkNotNull(placeId, "placeId is required");
         return withPlaceId(UUID.fromString(placeId));
      }

      public Builder withMsgKey(String msgKey) {
         this.msgKey = msgKey;
         return this;
      }

      public Builder addParams(Map<String, String> params) {
         if(params != null) {
            this.paramBuilder.putAll(params);
         }
         return this;
      }

      public Builder addParam(String key, String value) {
         this.paramBuilder.put(key, value);
         return this;
      }

      public Builder withPriority(String priority) {
         this.priority = priority;
         return this;
      }

      public Builder addCallTreeEntries(List<CallTreeEntry> callTreeEntries) {
         this.callTreeBuilder.addAll(callTreeEntries);
         return this;
      }

      public Builder addCallTreeEntries(CallTreeEntry... callTreeEntries) {
         this.callTreeBuilder.add(callTreeEntries);
         return this;
      }

      public Builder withSequentialDelaySecs(long sequentialDelaySecs) {
         this.sequentialDelaySecs = sequentialDelaySecs;
         return this;
      }

      public CallTreeContext build() {
         Preconditions.checkNotNull(incidentAddress, "incidentAddress is required");
         Preconditions.checkNotNull(placeId, "placeId is required");
         Preconditions.checkNotNull(msgKey, "msgKey is required");
         Preconditions.checkNotNull(priority, "priority is required");
         List<CallTreeEntry> entries = callTreeBuilder.build();
         Preconditions.checkArgument(entries.size() >= 1, "call tree must have at least one entry");
         return new CallTreeContext(incidentAddress, placeId, msgKey, paramBuilder.build(), priority, entries, sequentialDelaySecs);
      }
   }
}

