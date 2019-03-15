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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.model.Model;

@Singleton
public class ResponseCompleter {

   private static final Logger logger = LoggerFactory.getLogger(ResponseCompleter.class);

   private final ConcurrentMap<Address, Set<ResponseCorrelator>> pendingResponses = new ConcurrentHashMap<>();

   void completeResponse(Address device, ResponseCorrelator correlator) {
      this.pendingResponses.computeIfAbsent(device, key -> new HashSet<>()).add(correlator);
   }

   public void onMessage(Model m, MessageBody body) {
      if(!Capability.EVENT_VALUE_CHANGE.equals(body.getMessageType())) {
         logger.trace("ignoring non-value change event {}", body.getMessageType());
         return;
      }

      Set<ResponseCorrelator> correlators = pendingResponses.get(m.getAddress());
      if(correlators == null) {
         logger.trace("no pending responses for {}", m.getAddress());
         return;
      }

      Set<ResponseCorrelator> correlatorsClone = new HashSet<>(correlators);
      Set<ResponseCorrelator> done = respond(m, body, correlatorsClone);
      correlatorsClone.removeAll(done);
      if(correlatorsClone.isEmpty()) {
         logger.debug("removing correlators for {}, all responses have been completed", m.getAddress());
         pendingResponses.remove(m.getAddress(), correlators);
      }
   }

   private Set<ResponseCorrelator> respond(Model m, MessageBody body, Set<ResponseCorrelator> correlators) {
      Set<ResponseCorrelator> done = new HashSet<>();

      correlators.forEach(correlator -> {
         if(correlator.completed()) {
            done.add(correlator);
         } else {
            // filter out intermediate transition states for locks so we don't prematurely complete if not cheating or deferring
            Set<String> nonIntermediate = body.getAttributes().entrySet().stream()
               .filter(entry -> !isAttributeInIntermediateState(entry.getKey(), entry.getValue()))
               .map(Map.Entry::getKey)
               .collect(Collectors.toSet());
            if(correlator.allExpectedAttributesSeen(nonIntermediate)) {
               done.add(correlator);
               correlator.complete(m);
            }
         }
      });

      return done;
   }

   private boolean isAttributeInIntermediateState(String key, Object value) {
      if(DoorLockCapability.ATTR_LOCKSTATE.equals(key)) {
         return DoorLockCapability.LOCKSTATE_UNLOCKING.equals(value) || DoorLockCapability.LOCKSTATE_LOCKING.equals(value);
      }
      return false;
   }

}

