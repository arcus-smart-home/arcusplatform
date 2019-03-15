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
package com.iris.platform.subsystem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.PlaceDAO;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.PlatformPartition;

@Singleton
public class SubsystemLoader implements PartitionListener {
	public static final String NAME_EXECUTOR = "SubsystemLoader#executor";
	
	private static final Logger logger = LoggerFactory.getLogger(SubsystemLoader.class);
	
	private final Timer partitionLoadTimer;

	private final ExecutorService executor;
   private final PlaceDAO placeDao;
   private final SubsystemRegistry registry;
   
   private final boolean preloadPlaces;
   
   @Inject
   public SubsystemLoader(
   		@Named(NAME_EXECUTOR) ExecutorService executor,
   		PlaceDAO placeDao, 
   		SubsystemRegistry registry,
   		SubsystemConfig config
	) {
      this.partitionLoadTimer = IrisMetrics.metrics("service.subsystem").timer("partitionloadtime");
      
      this.executor = executor;
      this.placeDao = placeDao;
      this.registry = registry;
      this.preloadPlaces = config.isPreloadPlaces();
   }

   @Override
   public void onPartitionsChanged(PartitionChangedEvent event) {
      registry.clear();
      // FIXME cancel timeouts
      if(preloadPlaces) {
	      logger.info("Loading subsystems for [{}] partitions...", event.getPartitions().size());
	      for(PlatformPartition partition: event.getPartitions()) {
	         executor.execute(() -> loadSubsystemsByParition(partition));
	      }
      }
      else {
      	logger.info("Ignoring [{}] partition changes -- subsystem.place.preload=false", event.getPartitions().size());
      }
   }

   protected void loadSubsystemsByParition(PlatformPartition partition) {
      try(Timer.Context context = partitionLoadTimer.time()) {
         placeDao
            .streamPlaceAndAccountByPartitionId(partition.getId())
            .forEach((placeAndAccount) -> {
               for (Map.Entry<UUID,UUID> entry : placeAndAccount.entrySet()) {
                  registry.loadByPlace(entry.getKey(), entry.getValue());
               }
            });
      }
   }

}

