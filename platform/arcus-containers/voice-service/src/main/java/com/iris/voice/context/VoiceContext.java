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
package com.iris.voice.context;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.ModelStore;
import com.iris.voice.VoicePredicates;
import com.iris.voice.proactive.ProactiveCreds;

public class VoiceContext {

   private final UUID placeId;
   private final ModelStore modelStore;
   private final Set<String> assistants = ConcurrentHashMap.newKeySet();
   private final ConcurrentMap<String, ProactiveCreds> proactiveCreds = new ConcurrentHashMap<>();

   public VoiceContext(
      UUID placeId,
      ModelStore modelStore,
      Set<String> authorizedAssistants,
      Map<String, ProactiveCreds> proactiveCreds
   ) {
      this.placeId = placeId;
      this.modelStore = modelStore;
      this.assistants.addAll(authorizedAssistants);
      this.proactiveCreds.putAll(proactiveCreds);
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public Model getModelByAddress(Address addr) {
      return modelStore.getModelByAddress(addr);
   }

   public boolean hasAssistants() {
      return !assistants.isEmpty();
   }

   public boolean addAssistant(String assistant) {
      return assistants.add(assistant);
   }

   public void removeAssistant(String assistant) {
      assistants.remove(assistant);
      removeProactiveCreds(assistant);
   }

   public Stream<String> getAssistants() {
      return assistants.stream();
   }

   public void updateProactiveCreds(String assistant, ProactiveCreds creds) {
      proactiveCreds.put(assistant, creds);
   }

   public void removeProactiveCreds(String assistant) {
      proactiveCreds.remove(assistant);
   }

   public Optional<ProactiveCreds> getProactiveCreds(String assistant) {
      return Optional.ofNullable(proactiveCreds.get(assistant));
   }

   public ModelStore models() {
      return modelStore;
   }

   public <T> Stream<T> streamSupported(Predicate<? super Model> supported, Function<? super Model, Optional<T>> txfm) {
      return streamSupportedModels(supported)
            .map(txfm)
            .filter(Optional::isPresent)
            .map(Optional::get);
   }


   public Map<String,Map<String,Object>> query(Set<String> addresses, BiFunction<Model, Boolean, Map<String, Object>> txfm) {
      if(addresses == null || addresses.isEmpty()) {
         return ImmutableMap.of();
      }
      boolean hubOffline = isHubOffline();
      ImmutableMap.Builder<String, Map<String,Object>> deviceStates = ImmutableMap.builder();
      addresses.forEach(addrString -> {
         Address addr = Address.fromString(addrString);
         Model m = modelStore.getModelByAddress(addr);
         deviceStates.put(addrString, txfm.apply(m, hubOffline));
      });
      return deviceStates.build();
   }

   public Stream<Model> streamSupportedModels(Predicate<? super Model> supported) {
      return StreamSupport.stream(modelStore.getModels(supported::test).spliterator(), false);
   }

   public boolean isHubOffline() {
      Iterable<Model> hubs = modelStore.getModelsByType(HubCapability.NAMESPACE);
      return hubs != null && StreamSupport.stream(hubs.spliterator(), false).anyMatch(VoicePredicates::isHubOffline);
   }

}

