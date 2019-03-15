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
package com.iris.video.streaming.server.dao;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.video.VideoDao;
import com.iris.video.VideoRecording;
import com.iris.video.storage.VideoStorage;

@Singleton
public class VideoStreamingDao {

   private final VideoDao videoDao;
   private final VideoStorage videoStorage;

   @Inject
   public VideoStreamingDao(VideoDao videoDao, VideoStorage videoStorage) {
      this.videoDao = videoDao;
      this.videoStorage = videoStorage;

   }

   public URI getUri(String storageLocation, Date ts) throws Exception {
      return videoStorage.createPlaybackUri(storageLocation, ts);
   }

   @Nullable
   public VideoStreamingSession session(UUID id) {
      VideoRecording rec = videoDao.getVideoRecordingById(id);
      return rec == null ? null : new VideoStreamingSession(rec);
   }

   
}

