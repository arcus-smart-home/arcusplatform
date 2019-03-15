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
package com.iris.video.purge;

import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.cassandra.CassandraPlaceDAOModule;
import com.iris.core.messaging.kafka.KafkaMessageSender;
import com.iris.platform.partition.Partitioner;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.video.PreviewModule;
import com.iris.video.VideoDao;
import com.iris.video.VideoStorageModule;
import com.iris.video.cql.v2.CassandraVideoV2Module;
import com.iris.video.purge.dao.VideoPurgeDao;
import com.iris.video.purge.dao.v2.CassandraVideoPurgeV2Dao;
import com.iris.video.storage.PreviewStorage;
import com.iris.video.storage.VideoStorage;
import com.netflix.governator.annotations.Modules;

@Modules(include={ CassandraVideoV2Module.class, 
		VideoStorageModule.class, 
		PreviewModule.class,
		CassandraPlaceDAOModule.class})
public class VideoPurgeTaskModule extends AbstractIrisModule {
   
   @Override
   protected void configure() {
      bind(VideoPurgeTaskConfig.class);
      bind(VideoPurgeDao.class).to(CassandraVideoPurgeV2Dao.class);
   }
   
   @Provides
   @Singleton
   public ListeningExecutorService provideListeningExecutorService(VideoPurgeTaskConfig config) {
   	return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(config.getConcurrency()));
   }
   
   @Provides
   @Singleton
   public PurgeJob providePurgeJob(VideoPurgeTaskConfig config, 
   		VideoPurgeDao videoPurgeDao, 
   		VideoDao videoDao,
   		PlaceDAO placeDao,
   		PlatformMessageSender sender,
   		VideoStorage storage,
   		PreviewStorage previewStorage,
   		PlacePopulationCacheManager populationCacheMgr) {
      if(PurgeMode.PINNED_BASIC_PLACE.equals(config.getPurgeMode()) || PurgeMode.DELETED_PLACE.equals(config.getPurgeMode())){
         return new PurgeRecordingsForPlaceJob(videoDao, placeDao, config, sender, provideListeningExecutorService(config));
      }else{
      	return new PurgeDeletedRecordingJob(videoPurgeDao, config, sender, storage, previewStorage, populationCacheMgr, provideListeningExecutorService(config));
      }
   }
   
   
}

