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

import static com.iris.video.purge.VideoPurgeTaskMetrics.PURGED_RECORDING;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.iris.core.IrisAbstractApplication;
import com.iris.core.dao.cassandra.CassandraModule;
import com.iris.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.metricsreporter.builder.MetricsTopicReporterBuilderModule;
import com.iris.io.json.gson.GsonModule;

@Singleton
public class VideoPurgeTask extends IrisAbstractApplication {
   private static final Logger log = LoggerFactory.getLogger(VideoPurgeTask.class);
   
   private final PurgeJob theJob;
   
   @Inject
   public VideoPurgeTask(PurgeJob theJob) {
   	this.theJob = theJob;
   }

   
   

   @Override
   protected void start() throws Exception {
      try {
         long totalPurgeRows = theJob.doPurge();
         log.info("purge task complete after examining {} metadata rows, purged {} total recordings", totalPurgeRows, PURGED_RECORDING.getCount());
      } catch (Throwable th) {
         log.info("purge task exited abnormally:", th);
      } finally {
         System.exit(0);
      }
   }

   public static void main(String... args) {
      Collection<Class<? extends Module>> modules = Arrays.asList(
         VideoPurgeTaskModule.class,
         KafkaModule.class,
         CassandraModule.class,
         CassandraResourceBundleDAOModule.class,
         GsonModule.class,
         MetricsTopicReporterBuilderModule.class
      );

      IrisAbstractApplication.exec(VideoPurgeTask.class, modules, args);
   }
}

