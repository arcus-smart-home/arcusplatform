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

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.video.VideoRecording;

public interface VideoStorage {
   VideoStorageSession create(VideoRecording recording) throws Exception;
   VideoStorageSession create(UUID recordingId, UUID cameraId, UUID accountId, UUID placeId, @Nullable UUID personId, long ttlInSeconds) throws Exception;

   URI createPlaybackUri(String storagePath, Date ts) throws Exception;
   URI createPlaybackUri(URI storagePath, Date ts) throws Exception;

   void delete(String storagePath) throws Exception;
}

