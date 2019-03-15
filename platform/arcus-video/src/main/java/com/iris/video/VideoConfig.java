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

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.microsoft.azure.storage.StorageCredentials;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.KeyParser;

public class VideoConfig extends VideoDaoConfig{
   public static final String VIDEO_STORAGE_TYPE_FS = "fs";
   public static final String VIDEO_STORAGE_TYPE_AZURE = "azure";

   @Inject @Named("video.streaming.url")
   protected String videoStreamingUrl;

   @Inject @Named("video.download.url")
   protected String videoDownloadUrl;

   @Inject @Named("video.record.url")
   protected String videoRecordUrl;

   @Inject(optional = true) @Named("video.storage.type")
   protected String storageType = "fs";

   @Inject(optional = true) @Named("video.storage.fs.base_path")
   protected String storageFsBasePath = "/data/video/recordings";

   @Inject(optional = true) @Named("video.storage.azure.access.duration")
   protected long storageAzureAccessDuration = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);

   @Inject @Named("video.storage.azure.container")
   protected String storageAzureContainer;

   @Inject(optional = true) @Named("video.storage.azure.instrument")
   protected boolean storageAzureInstrument = false;

   @Inject(optional = true) @Named("video.storage.azure.buffer.size")
   protected int storageAzureBufferSize = -1;

   @Inject(optional = true) @Named("video.storage.azure.fetch.size")
   protected int storageAzureFetchSize = 16*1024;

   @Inject(optional = true) @Named("video.http.content.max.length")
   protected int videoHttpMaxContentLength = 64 * 1024;

   @Inject(optional = true) @Named("video.http.ssl.handshake.timeout")
   protected long videoSslHandshakeTimeout = 10L;

   @Inject(optional = true) @Named("video.http.ssl.close.notify.timeout")
   protected long videoSslCloseNotifyTimeout = 3L;

   

   @Inject
   protected ConfigurationProvider configProvider;

   public String getVideoStreamingUrl() {
      return videoStreamingUrl;
   }

   public void setVideoStreamingUrl(String videoStreamingUrl) {
      this.videoStreamingUrl = videoStreamingUrl;
   }

   public String getVideoDownloadUrl() {
      return videoDownloadUrl;
   }

   public void setVideoDownloadUrl(String videoDownloadUrl) {
      this.videoDownloadUrl = videoDownloadUrl;
   }

   public String getVideoRecordUrl() {
      return videoRecordUrl;
   }

   public void setVideoRecordUrl(String videoRecordUrl) {
      this.videoRecordUrl = videoRecordUrl;
   }

   public String getStorageType() {
      return storageType;
   }

   public void setStorageType(String storageType) {
      this.storageType = storageType;
   }

   public String getStorageFsBasePath() {
      return storageFsBasePath;
   }

   public void setStorageFsBasePath(String storageFsBasePath) {
      this.storageFsBasePath = storageFsBasePath;
   }

   public long getStorageAzureAccessDuration() {
      return storageAzureAccessDuration;
   }

   public void setStorageAzureAccessDuration(long storageAzureAccessDuration) {
      this.storageAzureAccessDuration = storageAzureAccessDuration;
   }

   public String getStorageAzureContainer() {
      return storageAzureContainer;
   }

   public void setStorageAzureContainer(String storageAzureContainer) {
      this.storageAzureContainer = storageAzureContainer;
   }

   public boolean isStorageAzureInstrument() {
      return storageAzureInstrument;
   }

   public void setStorageAzureInstrument(boolean storageAzureInstrument) {
      this.storageAzureInstrument = storageAzureInstrument;
   }

   public int getStorageAzureBufferSize() {
      return storageAzureBufferSize;
   }

   public void setStorageAzureBufferSize(int storageAzureBufferSize) {
      this.storageAzureBufferSize = storageAzureBufferSize;
   }

   public int getStorageAzureFetchSize() {
      return storageAzureFetchSize;
   }

   public void setStorageAzureFetchSize(int storageAzureFetchSize) {
      this.storageAzureFetchSize = storageAzureFetchSize;
   }

   public int getVideoHttpMaxContentLength() {
      return videoHttpMaxContentLength;
   }

   public void setVideoHttpMaxContentLength(int videoHttpMaxContentLength) {
      this.videoHttpMaxContentLength = videoHttpMaxContentLength;
   }

   public long getVideoSslHandshakeTimeout() {
      return videoSslHandshakeTimeout;
   }

   public void setVideoSslHandshakeTimeout(long videoSslHandshakeTimeout) {
      this.videoSslHandshakeTimeout = videoSslHandshakeTimeout;
   }

   public long getVideoSslCloseNotifyTimeout() {
      return videoSslCloseNotifyTimeout;
   }

   public void setVideoSslCloseNotifyTimeout(long videoSslCloseNotifyTimeout) {
      this.videoSslCloseNotifyTimeout = videoSslCloseNotifyTimeout;
   }

   public List<StorageCredentials> getStorageAzureAccounts() {
      List<StorageCredentials> result = new ArrayList<>();

      for (int i = 1; true; ++i) {
         String rawAccount = "video.storage.azure.account" + i;
         ConfigurationKey confAccount = new ConfigurationKey(rawAccount, KeyParser.parse(rawAccount));
         Supplier<String> supAccount = configProvider.getStringSupplier(confAccount, null);
         String account = (supAccount == null) ? null : supAccount.get();

         if (account == null || account.trim().isEmpty()) {
            break;
         }

         try {
            StorageCredentials creds = StorageCredentials.tryParseCredentials(account);
            if (creds == null) {
               throw new RuntimeException("invalid azure storage credentials");
            }

            result.add(creds);
         } catch (InvalidKeyException ex) {
            throw new RuntimeException(ex);
         }
      }

      return result;
   }

   
}

