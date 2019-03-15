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

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModelStore;
import com.iris.platform.model.ModelDao;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.IrisUUID;
import com.iris.voice.VoiceConfig;
import com.iris.voice.VoiceProvider;
import com.iris.voice.exec.ResponseCompleter;
import com.iris.voice.proactive.ProactiveCredsDAO;
import com.iris.voice.proactive.ProactiveReporter;

@Singleton
public class VoiceContextExecutorRegistry implements PartitionListener {

   private static final Logger logger = LoggerFactory.getLogger(VoiceContextExecutorRegistry.class);

   private static final Set<String> MODELS = ImmutableSet.of(
      PlaceCapability.NAMESPACE,
      DeviceCapability.NAMESPACE,
      HubCapability.NAMESPACE,
      SceneCapability.NAMESPACE
   );

   private final ExecutorService executor;
   private final VoiceDAO voiceDao;
   private final ModelDao modelDao;
   private final ProactiveCredsDAO proactiveCredsDao;
   private final VoiceConfig config;
   private final ProactiveReporter proactiveReporter;
   private final ResponseCompleter responseCompleter;
   private final Set<UUID> placesWithVoice = ConcurrentHashMap.newKeySet();
   private final LoadingCache<UUID, VoiceContextExecutor> contexts;
   private final Set<VoiceProvider> providers;


   @SuppressWarnings("unchecked")
   @Inject
   public VoiceContextExecutorRegistry(
      @Named(VoiceConfig.NAME_EXECUTOR) ExecutorService executor,
      VoiceDAO voiceDao,
      ModelDao modelDao,
      ProactiveCredsDAO proactiveCredsDao,
      VoiceConfig config,
      ProactiveReporter proactiveReporter,
      ResponseCompleter responseCompleter,
      Partitioner partitioner,
      Set<VoiceProvider> providers
   ) {
      this.executor = executor;
      this.voiceDao = voiceDao;
      this.modelDao = modelDao;
      this.proactiveCredsDao = proactiveCredsDao;
      this.config = config;
      this.proactiveReporter = proactiveReporter;
      this.responseCompleter = responseCompleter;
      this.providers = Collections.unmodifiableSet(providers);
      contexts = config.cacheBuilder()
            .removalListener((RemovalListener<UUID, VoiceContextExecutor>) this::onRemoved)
            .build(new CacheLoader<UUID, VoiceContextExecutor>() {
               @Override
               public VoiceContextExecutor load(@NonNull UUID key) throws Exception {
                  return loadContext(key);
               }
            });
      partitioner.addPartitionListener(this);
   }

   public VoiceContextExecutor get(UUID placeId) {
      try {
         return contexts.get(placeId);
      } catch(Exception e) {
         logger.error("failed to get service executor for {}", placeId, e);
         throw new RuntimeException(e);
      }
   }

   public Optional<VoiceContextExecutor> getOptional(UUID placeId) {
      if(placesWithVoice.contains(placeId)) {
         return Optional.of(get(placeId));
      }
      return Optional.empty();
   }

   public void remove(UUID placeId) {
      placesWithVoice.remove(placeId);
      contexts.invalidate(placeId);
   }

   public void add(UUID placeId) {
      placesWithVoice.add(placeId);
      if(config.isCachePreload()) {
         get(placeId);
      }
   }

   @Override
   public void onPartitionsChanged(PartitionChangedEvent event) {
      placesWithVoice.clear();
      contexts.invalidateAll();
      event.getPartitions().forEach(this::submitLoadByPartition);
   }

   private void submitLoadByPartition(PlatformPartition partition) {
      executor.execute(() -> loadByPartition(partition));
   }

   private void loadByPartition(PlatformPartition partition) {
      try(Timer.Context ctxt = VoiceContextMetrics.startPartitionLoadTime()) {
         voiceDao.streamPlacesByPartition(partition).forEach(this::add);
      }
   }

   private void onRemoved(RemovalNotification notification) {
      if(notification.getCause() != RemovalCause.EXPLICIT) {
         logger.info("voice context for {} removed from cache because {}", notification.getKey(), notification.getCause());
      }
   }

   private VoiceContextExecutor loadContext(UUID placeId) {
      try(Timer.Context ctxt = VoiceContextMetrics.startContextLoadTime()) {
         Collection<Model> models = modelDao.loadModelsByPlace(placeId, MODELS);
         // make double-dog sure that if a context is loaded the place id is in the placesWithVoice set
         placesWithVoice.add(placeId);
         logger.trace("loaded context for {} resulted in {} models", placeId, models.size());
         SimpleModelStore sms = new SimpleModelStore();
         sms.setTrackedTypes(MODELS);
         sms.addModel(models.stream().filter(Objects::nonNull).map(Model::toMap).collect(Collectors.toList()));
         sms.addListener(this::onModelEvent);
         VoiceContext context = new VoiceContext(
            placeId,
            sms,
            voiceDao.readAssistants(placeId),
            proactiveCredsDao.credentialsForPlace(placeId)
         );
         return new VoiceContextExecutor(context, proactiveReporter, responseCompleter, providers, config.getPerPlaceQueueDepth());
      }
   }

   private void onModelEvent(ModelEvent event) {
      if(!(event instanceof ModelRemovedEvent)) {
         return;
      }
      Model m = ((ModelRemovedEvent) event).getModel();
      if(Objects.equals(PlaceCapability.NAMESPACE, m.getType())) {
         logger.debug("invalidating voice context cache for {}", m.getId());
         remove(IrisUUID.fromString(m.getId()));
         // TODO:  remove proactive creds for place
      }
   }
}

