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
package com.iris.video.storage;

import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_BAD_ACCOUNT_NAME;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_BAD_URI;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_BYTES;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_CLOSE_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_CLOSE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_CREATE_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_CREATE_PLAYBACK_URI_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_CREATE_PLAYBACK_URI_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_CREATE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_DELETE_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_DELETE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_DOWNLOAD_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_DOWNLOAD_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_EXISTING_CREATE_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_EXISTING_CREATE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_FLUSH_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_FLUSH_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_NO_CONTAINER;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_NO_LOCATION;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_OPEN_READ_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_OPEN_READ_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_OPEN_WRITE_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_OPEN_WRITE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_WRITE_BYTE_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_WRITE_BYTE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_WRITE_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_STORAGE_AZURE_WRITE_SUCCESS;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.video.VideoRecording;
import com.iris.video.cql.v2.VideoV2Util;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.SharedAccessBlobHeaders;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;

public class VideoStorageAzure implements VideoStorage {
   private static final Logger log = LoggerFactory.getLogger(VideoStorageAzure.class);

   private final List<CloudBlobContainer> containers;
   private final Map<String,CloudBlobClient> accounts;
   private final long accessDurationInMs;
   private final boolean instrumentOs;
   private final int bufferOsSize;
   private final int fetchIsSize;

   public VideoStorageAzure(List<StorageCredentials> accounts, String container, long accessDurationInMs, boolean instrumentOs, int bufferOsSize, int fetchIsSize) {
      this.containers = new ArrayList<>();
      this.accounts = new HashMap<>();
      this.accessDurationInMs = accessDurationInMs;
      this.instrumentOs = instrumentOs;
      this.bufferOsSize = bufferOsSize;
      this.fetchIsSize = fetchIsSize;

      for (StorageCredentials account : accounts) {
         try {
            CloudStorageAccount storage = new CloudStorageAccount(account,true);
            this.accounts.put(account.getAccountName(), storage.createCloudBlobClient());

            CloudBlobClient client = storage.createCloudBlobClient();
            this.containers.add(getStorageContainer(client,container));
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }

      log.info("configured azure storage with {} accounts", accounts.size());
   }

   @Override
   public VideoStorageSession create(VideoRecording recording) throws Exception {
      long startTime = System.nanoTime();

      try {
         if (recording == null || recording.storage == null) {
            VIDEO_STORAGE_AZURE_NO_LOCATION.inc();
            throw new Exception("no storage location");
         }

         CloudBlobContainer foundContainer = null;
         for (CloudBlobContainer container : containers) {
            URI uri = container.getUri();
            String uriString = uri.toString();
            if (recording.storage.startsWith(uriString)) {
               foundContainer = container;
               break;
            }
         }

         if (foundContainer == null) {
            VIDEO_STORAGE_AZURE_NO_CONTAINER.inc();
            throw new Exception("no azure container for storage location");
         }

         CloudBlobDirectory dir = foundContainer.getDirectoryReference(recording.placeId.toString());
         CloudAppendBlob blob = dir.getAppendBlobReference(recording.recordingId.toString());

         blob.getProperties().setCacheControl("no-cache");
         blob.getProperties().setContentType("video/mp2t");
         long expiration = recording.expiration;
         long ttlInSeconds = VideoV2Util.createActualTTL(recording.recordingId, expiration);
         
         VideoStorageSession result = new AzureStorageSession(recording.recordingId, recording.cameraId, recording.accountId, recording.placeId, ttlInSeconds, recording.personId, blob);

         VIDEO_STORAGE_AZURE_EXISTING_CREATE_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return result;
      } catch (Exception ex) {
         VIDEO_STORAGE_AZURE_EXISTING_CREATE_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   @Override
   public VideoStorageSession create(UUID recordingId, UUID cameraId, UUID accountId, UUID placeId, @Nullable UUID personId, long ttlInSeconds) throws Exception {
      long startTime = System.nanoTime();

      try {
         CloudBlobContainer container = getRandomContainer(recordingId, cameraId, placeId);
         CloudBlobDirectory dir = container.getDirectoryReference(placeId.toString());
         CloudAppendBlob blob = dir.getAppendBlobReference(recordingId.toString());

         blob.getProperties().setCacheControl("no-cache");
         blob.getProperties().setContentType("video/mp2t");

         blob.setStreamWriteSizeInBytes(4*1024*1024);
         VideoStorageSession result = new AzureStorageSession(recordingId, cameraId, accountId, placeId, ttlInSeconds, personId,  blob);

         VIDEO_STORAGE_AZURE_CREATE_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return result;
      } catch (Exception ex) {
         VIDEO_STORAGE_AZURE_CREATE_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   @Override
   public URI createPlaybackUri(String storagePath, Date ts) throws Exception {
      return createPlaybackUri(URI.create(storagePath), ts);
   }

   @Override
   public URI createPlaybackUri(URI storagePath, Date ts) throws Exception {
      long startTime = System.nanoTime();

      try {
         String accountName = getAccountName(storagePath);
         CloudBlobClient client = accounts.get(accountName);
         if (client == null) {
            VIDEO_STORAGE_AZURE_BAD_ACCOUNT_NAME.inc();
            throw new Exception("unknown account name: " + accountName);
         }

         CloudBlockBlob blob = new CloudBlockBlob(storagePath, client);

         SharedAccessBlobHeaders headers = new SharedAccessBlobHeaders();
         headers.setContentType("video/mp2t");
         headers.setCacheControl("no-cache");

         Date start = new Date(ts.getTime() - accessDurationInMs);

         SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
         policy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));
         policy.setSharedAccessStartTime(start);
         policy.setSharedAccessExpiryTime(ts);

         String sig = blob.generateSharedAccessSignature(policy, headers, null);
         String query = storagePath.getQuery();

         URI result;
         if (query != null && !query.isEmpty()) {
            result = URI.create(storagePath + "&" + sig);
         } else {
            result = URI.create(storagePath + "?" + sig);
         }

         VIDEO_STORAGE_AZURE_CREATE_PLAYBACK_URI_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return result;
      } catch (Exception ex) {
         VIDEO_STORAGE_AZURE_CREATE_PLAYBACK_URI_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   @Override
   public void delete(String storagePath) throws Exception {
      long startTime = System.nanoTime();
      URI uri = URI.create(storagePath);

      try {
         String accountName = getAccountName(uri);
         CloudBlobClient client = accounts.get(accountName);
         if (client == null) {
            VIDEO_STORAGE_AZURE_BAD_ACCOUNT_NAME.inc();
            throw new Exception("unknown account name: " + accountName);
         }

         CloudBlockBlob blob = new CloudBlockBlob(uri, client);
         blob.deleteIfExists();

         VIDEO_STORAGE_AZURE_DELETE_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
      } catch (Exception ex) {
         VIDEO_STORAGE_AZURE_DELETE_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   private String getAccountName(URI uri) {
      String host = uri.getHost();

      int idx = host.indexOf('.');
      if (idx <= 0) {
         VIDEO_STORAGE_AZURE_BAD_URI.inc();
         throw new RuntimeException("storage uri not in azure blob storage");
      }

      return host.substring(0, idx);
   }

   private CloudBlobContainer getRandomContainer(UUID recordingId, UUID cameraId, UUID placeId) {
      int idx = ThreadLocalRandom.current().nextInt(0, containers.size());
      return containers.get(idx);
   }

   private static CloudBlobContainer getStorageContainer(CloudBlobClient client, String container) {
      try {
         CloudBlobContainer cnt = client.getContainerReference(container);
         cnt.createIfNotExists();

         return cnt;
      } catch (Exception ex) {
         VIDEO_STORAGE_AZURE_NO_CONTAINER.inc();
         throw new RuntimeException(ex);
      }
   }

   final class AzureStorageSession extends AbstractVideoStorageSession {
      private final CloudAppendBlob blob;

      public AzureStorageSession(UUID recordingId, UUID cameraId, UUID accountId, UUID placeId, long ttlInSeconds, @Nullable UUID personId, CloudAppendBlob blob) {
         super(recordingId, cameraId, accountId, placeId, personId, ttlInSeconds);
         this.blob = blob;
      }

      @Override
      public String location() throws Exception {
         return blob.getUri().toString();
      }

      @Override
      public OutputStream output() throws Exception {
         long startTime = System.nanoTime();
         try {
            OutputStream result = blob.openWriteNew();
            if (instrumentOs) {
               result = new InstrumentedAzureOutputStream(result);
            }

            if (bufferOsSize > 0) {
               result = new BufferedOutputStream(result, bufferOsSize);
            }

            VIDEO_STORAGE_AZURE_OPEN_WRITE_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            return result;
         } catch (Exception ex) {
            VIDEO_STORAGE_AZURE_OPEN_WRITE_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            throw ex;
         }
      }

      @Override
      public InputStream input() throws Exception {
         long startTime = System.nanoTime();
         try {
            blob.setStreamMinimumReadSizeInBytes(fetchIsSize);
            InputStream result = blob.openInputStream();

            VIDEO_STORAGE_AZURE_OPEN_READ_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            return result;
         } catch (Exception ex) {
            VIDEO_STORAGE_AZURE_OPEN_READ_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            throw ex;
         }
      }

      @Override
      public void read(byte[] buf, long offset, long bytes, int bufferOffset) throws Exception {
         long startTime = System.nanoTime();
         try {
            blob.downloadRangeToByteArray(offset, bytes, buf, bufferOffset);
            VIDEO_STORAGE_AZURE_DOWNLOAD_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         } catch (Exception ex) {
            VIDEO_STORAGE_AZURE_DOWNLOAD_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         }
      }
   }

   static final class InstrumentedAzureOutputStream extends OutputStream {
      private final OutputStream delegate;

      public InstrumentedAzureOutputStream(OutputStream delegate) {
         this.delegate = delegate;
      }

      @Override
      public void write(int b) throws IOException {
         try {
            delegate.write(b);
            VIDEO_STORAGE_AZURE_BYTES.inc();
            VIDEO_STORAGE_AZURE_WRITE_BYTE_SUCCESS.inc();
         } catch (Exception ex) {
            VIDEO_STORAGE_AZURE_WRITE_BYTE_FAIL.inc();
            throw ex;
         }
      }

      @Override
      public void write(byte[] b) throws IOException {
         try {
            delegate.write(b);
            VIDEO_STORAGE_AZURE_BYTES.inc(b == null ? 0 : b.length);
            VIDEO_STORAGE_AZURE_WRITE_SUCCESS.inc();
         } catch (Exception ex) {
            VIDEO_STORAGE_AZURE_WRITE_FAIL.inc();
            throw ex;
         }
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
         try {
            delegate.write(b, off, len);
            VIDEO_STORAGE_AZURE_BYTES.inc(len);
            VIDEO_STORAGE_AZURE_WRITE_SUCCESS.inc();
         } catch (Exception ex) {
            VIDEO_STORAGE_AZURE_WRITE_FAIL.inc();
            throw ex;
         }
      }

      @Override
      public void flush() throws IOException {
         try {
            delegate.flush();
            VIDEO_STORAGE_AZURE_FLUSH_SUCCESS.inc();
         } catch (Exception ex) {
            VIDEO_STORAGE_AZURE_FLUSH_FAIL.inc();
            throw ex;
         }
      }

      @Override
      public void close() throws IOException {
         try {
            delegate.close();
            VIDEO_STORAGE_AZURE_CLOSE_SUCCESS.inc();
         } catch (Exception ex) {
            VIDEO_STORAGE_AZURE_CLOSE_FAIL.inc();
            throw ex;
         }
      }
   }
}

