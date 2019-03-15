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

import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

public abstract class AbstractVideoStorageSession implements VideoStorageSession {
   protected final UUID recordingId;
   protected final UUID cameraId;
   protected final UUID accountId;
   protected final UUID placeId;
   protected final long ttlInSeconds;

   @Nullable
   protected final UUID personId;

   public AbstractVideoStorageSession(UUID recordingId, UUID cameraId, UUID accountId, UUID placeId, @Nullable UUID personId, long ttlInSeconds) {
      this.recordingId = recordingId;
      this.cameraId = cameraId;
      this.accountId = accountId;
      this.placeId = placeId;
      this.personId = personId;
      this.ttlInSeconds = ttlInSeconds;
   }

   @Override
   public UUID getCameraId() {
      return cameraId;
   }

   @Override
   @Nullable
   public UUID getPersonId() {
      return personId;
   }

   @Override
   public UUID getPlaceId() {
      return placeId;
   }

   @Override
   public UUID getAccountId() {
      return accountId;
   }

   @Override
   public UUID getRecordingId() {
      return recordingId;
   }

	@Override
	public long getRecordingTtlInSeconds() {
		return ttlInSeconds;
	}
   
   
}

