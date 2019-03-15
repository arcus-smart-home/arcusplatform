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
package com.iris.messages.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.jdt.annotation.NonNull;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;

public class ModelReportEvent extends ModelEvent {

   public static class ValueChange {
      private final Object oldValue;
      private final Object newValue;

      public ValueChange(final Object oldValue, final Object newValue) {
         this.oldValue = oldValue;
         this.newValue = newValue;
      }

      public Object getOldValue() {
         return oldValue;
      }

      public Object getNewValue() {
         return newValue;
      }

      @Override
      public String toString()
      {
         return new ToStringBuilder(this)
            .append("oldValue", oldValue)
            .append("newValue", newValue)
            .toString();
      }
   }

   private final Address address;
   private final Map<String, ValueChange> changes;

   private ModelReportEvent(final Address address, Map<String, ValueChange> changes) {
      this.address = address;
      this.changes = changes;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   public Map<String, ValueChange> getChanges() {
      return changes;
   }

   @Override
   public boolean equals(Object o) {
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;

      ModelReportEvent that = (ModelReportEvent) o;

      if(!address.equals(that.address)) return false;
      return changes.equals(that.changes);
   }

   @Override
   public int hashCode() {
      int result = address.hashCode();
      result = 31 * result + changes.hashCode();
      return result;
   }

   @Override
   public String toString()
   {
      return new ToStringBuilder(this)
         .append("address", address)
         .append("changes", changes)
         .toString();
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private Address address;
      private Map<String, ValueChange> changes = new HashMap<>();

      private Builder() {}

      public Builder withAddress(@NonNull Address address) {
         this.address = address;
         return this;
      }

      public Builder addChange(String attrName, Object oldValue, Object newValue) {
         changes.put(attrName, new ValueChange(oldValue, newValue));
         return this;
      }

      public ModelReportEvent build() {
         return new ModelReportEvent(address, ImmutableMap.copyOf(changes));
      }
   }
}

