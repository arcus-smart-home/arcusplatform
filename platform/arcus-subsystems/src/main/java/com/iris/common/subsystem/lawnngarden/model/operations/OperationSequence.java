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
package com.iris.common.subsystem.lawnngarden.model.operations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OperationSequence {

   private final String zone;
   private final List<PendingOperation> pendingOperations;
   private int index = 0;

   private OperationSequence(String zone, List<PendingOperation> operations) {
      this.zone = zone;
      pendingOperations = new ArrayList<>(operations);
   }

   public String zone() {
      return zone;
   }

   public synchronized List<PendingOperation> operations() {
      return pendingOperations;
   }

   public synchronized boolean completed() {
      return index == pendingOperations.size();
   }

   public synchronized PendingOperation next() {
      if(completed()) {
         return null;
      }
      return pendingOperations.get(index++);
   }

   public static class Builder {
      private String zone;
      private final List<PendingOperation> pendingOperations = new LinkedList<>();

      private Builder() {
      }

      public Builder withZone(String zone) {
         this.zone = zone;
         return this;
      }

      public Builder addOperation(PendingOperation op) {
         this.pendingOperations.add(op);
         return this;
      }

      public Builder withOperations(List<PendingOperation> ops) {
         pendingOperations.clear();
         if(ops != null) {
            pendingOperations.addAll(ops);
         }
         return this;
      }

      public OperationSequence build() {
         return new OperationSequence(zone, pendingOperations);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(OperationSequence op) {
      Builder builder = builder();
      if(op != null) {
         builder
            .withOperations(op.pendingOperations);
      }
      return builder;
   }
}

