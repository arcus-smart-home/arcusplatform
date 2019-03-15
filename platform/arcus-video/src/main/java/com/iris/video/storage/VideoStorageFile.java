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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.video.VideoRecording;
import com.iris.video.cql.v2.VideoV2Util;

public class VideoStorageFile implements VideoStorage {
   private final File dir;

   public VideoStorageFile(String dir) {
      this(new File(dir));
   }

   public VideoStorageFile(File dir) {
      this.dir = dir;
      this.dir.mkdirs();
   }

   @Override
   public VideoStorageSession create(VideoRecording recording) throws Exception {
      File placeDir = new File(dir, recording.placeId.toString());
      File output = new File(placeDir, recording.recordingId.toString());
      long ttlInSeconds = VideoV2Util.createActualTTL(recording.recordingId, recording.expiration);
      return new FileStorageSession(recording.recordingId, recording.cameraId, recording.accountId, recording.placeId, ttlInSeconds, recording.personId, output);
   }

   @Override
   public VideoStorageSession create(UUID recordingId, UUID cameraId, UUID accountId, UUID placeId, @Nullable UUID personId, long ttlInSeconds) throws Exception {
      File placeDir = new File(dir, placeId.toString());
      placeDir.mkdirs();

      File output = new File(placeDir, recordingId.toString());
      return new FileStorageSession(recordingId, cameraId, accountId, placeId, ttlInSeconds, personId, output);
   }

   @Override
   public URI createPlaybackUri(String storagePath, Date ts) throws Exception {
      return createPlaybackUri(URI.create(storagePath), ts);
   }

   @Override
   public URI createPlaybackUri(URI storagePath, Date ts) throws Exception {
      return storagePath;
   }

   @Override
   public void delete(String storagePath) throws Exception {
      File path = new File(storagePath);
      path.delete();

      if (path.exists()) {
         throw new Exception("could not delete file");
      }
   }

   static final class FileStorageSession extends AbstractVideoStorageSession {
      private final File path;

      public FileStorageSession(UUID recordingId, UUID cameraId, UUID accountId, UUID placeId, long ttlInSeconds, @Nullable UUID personId, File path) {
         super(recordingId, cameraId, accountId, placeId, personId, ttlInSeconds);
         this.path = path;
      }

      @Override
      public String location() throws Exception {
         return "file://" + path.getAbsolutePath();
      }

      @Override
      public OutputStream output() throws Exception {
         return new BufferedOutputStream(new FileOutputStream(path));
      }

      @Override
      public InputStream input() throws Exception {
         return new BufferedInputStream(new FileInputStream(path));
      }

      @Override
      public void read(byte[] buf, long offset, long bytes, int bufferOffset) throws Exception {
         try(RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            raf.seek(offset);
            raf.read(buf, bufferOffset, (int) bytes);
         }
      }
   }
}

