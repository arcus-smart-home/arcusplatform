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
package com.iris.video.service.quota;

import static com.iris.video.service.VideoServiceMetrics.QUOTA_ENFORCEMENT_DELETES;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.model.Place;
import com.iris.video.VideoRecordingSize;
import com.iris.video.service.dao.VideoServiceDao;

public class VideoQuotaEnforcerDeleteOldest extends AbstractVideoQuotaEnforcer {
   private static final Logger log = LoggerFactory.getLogger(VideoQuotaEnforcerDeleteOldest.class);
   private final int maxDeletesAllowed;

   public VideoQuotaEnforcerDeleteOldest(PlatformMessageBus platformBus, VideoServiceDao videoDao,
      boolean defaultDecisionOnFailure, int maxDeletesAllowed) {
      super(platformBus, videoDao, defaultDecisionOnFailure);
      this.maxDeletesAllowed = maxDeletesAllowed;
   }

   @Nullable
   @Override
   protected Iterable<VideoRecordingSize> getRecordingsToDelete(Place place, long used, long allowed, LongConsumer quotaUpdater) throws Exception {
      long newUsed = used;

      List<VideoRecordingSize> delete = new ArrayList<>();
      Iterator<VideoRecordingSize> it = videoDao.streamRecordingIdsForPlace(place.getId(), false).iterator();

      // We keep deleting non-favorited recordings until:
      //  1) we run out of videos
      //  2) we are below the quota
      //  3) we are at the maximum number of deletes allowed per new recording
      while(
            it.hasNext() && 
            delete.size() < maxDeletesAllowed && 
            newUsed > allowed
      ) {
         VideoRecordingSize recording = it.next();

         if (delete.size() == maxDeletesAllowed || newUsed < allowed) {
            break;
         }

         // Add this recording to the set of recordings to delete and adjust the
         // currently used space to reflect its deletion.
         delete.add(recording);
         newUsed -= recording.getSize();
      }

      // If we reach the maximum deletes then we will allow the new recording regardless
      // of whether there is quota space available or not.
      if (delete.size() == maxDeletesAllowed || newUsed < allowed) {
         QUOTA_ENFORCEMENT_DELETES.update(delete.size());
         quotaUpdater.accept(used-newUsed);
         return delete;
      }

      // If we went through all of the recordings and could not find enough to delete
      // then we deny the new recording without deleting anything.
      log.info("video quota enforcer denying new recording because not enough space could be freed: deletable={}, used={}, allowed={}, updated used={}", delete, used, allowed, newUsed);
      return null;
   }

}

