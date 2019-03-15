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
package com.iris.video;

import java.util.concurrent.ExecutorService;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.util.ThreadPoolBuilder;
import com.iris.video.storage.PreviewStorage;
import com.iris.video.storage.PreviewStorageAzure;
import com.iris.video.storage.PreviewStorageFile;
import com.iris.video.storage.PreviewStorageNull;

public class PreviewModule extends AbstractIrisModule {

   public PreviewModule() {
   }

   @Override
   protected void configure() {
   }

   @Provides @Singleton
   public PreviewStorage provideSnapshotStorage(PreviewConfig config) {
      switch (config.getStorageType()) {
      case PreviewConfig.PREVIEWS_STORAGE_TYPE_FS:
         return new PreviewStorageFile(config.getStorageFsBasePath());
      case PreviewConfig.PREVIEWS_STORAGE_TYPE_AZURE:
          return new PreviewStorageAzure(config.getStorageAzureAccounts(),config.getStorageAzureContainer(),previewImageExecutor(config));
      case PreviewConfig.PREVIEWS_STORAGE_TYPE_NULL:
         return new PreviewStorageNull();
      default:
         throw new RuntimeException("unknown video storage type: " + config.getStorageType());
      }
   }
   
   @Provides
   @Singleton
   public ExecutorService previewImageExecutor(PreviewConfig config) {
      return new ThreadPoolBuilder()
         .withBlockingBacklog()
         .withMaxPoolSize(config.getStorageAzureMaxThreads())
         .withKeepAliveMs(config.getStorageAzureKeepAliveMs())
         .withNameFormat("preview-image-writer-%d")
         .withMetrics("preview.azure")
         .build();
   }
}

