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
package com.iris.video.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.cassandra.CassandraPlaceDAOModule;
import com.iris.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.gson.GsonModule;
import com.iris.population.PlacePopulationCacheModule;
import com.iris.video.PreviewModule;
import com.iris.video.cql.v2.CassandraVideoV2Module;
import com.iris.video.recording.CameraDeletedListener;
import com.iris.video.recording.PlaceServiceLevelCacheModule;
import com.iris.video.recording.PlaceServiceLevelDowngradeListener;
import com.iris.video.service.dao.VideoServiceDao;
import com.iris.video.service.quota.VideoQuotaEnforcer;
import com.iris.video.service.quota.VideoQuotaEnforcerAllowAll;
import com.iris.video.service.quota.VideoQuotaEnforcerDeleteOldest;
import com.iris.video.service.quota.VideoQuotaEnforcerDenyAll;
import com.iris.video.service.quota.VideoQuotaEnforcerDenyAllRecordings;
import com.iris.video.service.quota.VideoQuotaEnforcerDisallowNew;
import com.netflix.governator.annotations.Modules;

@Modules(include={
      KafkaModule.class,
      CassandraResourceBundleDAOModule.class,
      CassandraPlaceDAOModule.class,
      CassandraVideoV2Module.class,
      GsonModule.class,
      VideoModule.class,
      PreviewModule.class,
      PlacePopulationCacheModule.class,
      PlaceServiceLevelCacheModule.class
})
public class VideoServiceModule extends AbstractIrisModule {
   private static final Logger log = LoggerFactory.getLogger(VideoServiceModule.class);

   @Inject(optional = true) @Named("video.quota.enforcer.premium")
   private String premiumVideoQuotaEnforcer = "allow.all";

   @Inject(optional = true) @Named("video.quota.enforcer.basic")
   private String basicVideoQuotaEnforcer = "allow.all";

   @Inject @Named("video.quota.enforcer.oldest.maxdelete")
   private int videoQuotaEnforcerMaxDelete;

   @Inject(optional = true) @Named("video.quota.enforcer.allow.on.fail")
   private boolean videoQuotaEnforcerAllowOnFail = true;

   @Override
   protected void configure() {
   	bind(PlaceServiceLevelDowngradeListener.class).asEagerSingleton();
   	bind(CameraDeletedListener.class).asEagerSingleton();
   }

   @Provides @Named("video.quota.enforcer.impl.premium")
   public VideoQuotaEnforcer provideVideoServiceQuotaEnforcerPremium(PlatformMessageBus platformBus, VideoServiceDao videoDao) {
      return getQuotaEnforcer("premium", premiumVideoQuotaEnforcer, platformBus, videoDao);
   }

   @Provides @Named("video.quota.enforcer.impl.basic")
   public VideoQuotaEnforcer provideVideoServiceQuotaEnforcerBasic(PlatformMessageBus platformBus, VideoServiceDao videoDao) {
      return getQuotaEnforcer("basic", basicVideoQuotaEnforcer, platformBus, videoDao);
   }

   @Provides
   public VideoQuotaEnforcer getQuotaEnforcer(String name, String type, PlatformMessageBus platformBus, VideoServiceDao videoDao) {
      switch (type) {
      case "delete.oldest":
         log.info("video quota enforcement for {}: deleting oldest recordings", name);
         return new VideoQuotaEnforcerDeleteOldest(platformBus, videoDao, videoQuotaEnforcerAllowOnFail, videoQuotaEnforcerMaxDelete);

      case "deny.new":
         log.info("video quota enforcement for {}: denying new recordings", name);
         return new VideoQuotaEnforcerDisallowNew();

      case "deny.all":
         log.info("video quota enforcement for {}: denying all recordings and streams", name);
         return new VideoQuotaEnforcerDenyAll();

      case "deny.all.recordings":
         log.info("video quota enforcement for {}: denying all recordings", name);
         return new VideoQuotaEnforcerDenyAllRecordings();

      case "allow.all":
      default:
         if ("allow.all".equals(type)) {
            log.info("video quota enforcement for {}: allowing all recordings", name);
         } else {
            log.info("video quota enforcement for {}: unknown enforcer {}, defaulting to allowing all recordings", name, type);
         }

         return new VideoQuotaEnforcerAllowAll();
      }
   }
}

