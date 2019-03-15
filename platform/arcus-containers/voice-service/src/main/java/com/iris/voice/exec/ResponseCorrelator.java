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
package com.iris.voice.exec;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Model;

class ResponseCorrelator {

   private static final Logger logger = LoggerFactory.getLogger(ResponseCorrelator.class);


   private final String correlationId;
   private final Set<String> expectedAttributes;
   private final SettableFuture<Pair<Model, Optional<PlatformMessage>>> future;

   ResponseCorrelator(String correlationId, Set<String> expectedAttributes, SettableFuture<Pair<Model, Optional<PlatformMessage>>> future) {
      this.correlationId = correlationId;
      this.expectedAttributes = new HashSet<>(expectedAttributes);
      this.future = future;
   }

   boolean allExpectedAttributesSeen(Set<String> changedAttribues) {
      expectedAttributes.removeAll(changedAttribues);
      return expectedAttributes.isEmpty();
   }

   boolean completed() {
      return future.isDone();
   }

   void complete(Model m) {
      if(!future.isDone()) {
         logger.debug("completing {} because all expected attribute updates have been seen and the future is not done", correlationId);
         future.set(new ImmutablePair<>(m, Optional.empty()));
      } else {
         logger.debug("completing {} is a no op because the future is done", correlationId);
      }
   }

   @Override
   public boolean equals(Object o) {
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;

      ResponseCorrelator that = (ResponseCorrelator) o;

      return correlationId != null ? correlationId.equals(that.correlationId) : that.correlationId == null;
   }

   @Override
   public int hashCode() {
      return correlationId != null ? correlationId.hashCode() : 0;
   }
}

