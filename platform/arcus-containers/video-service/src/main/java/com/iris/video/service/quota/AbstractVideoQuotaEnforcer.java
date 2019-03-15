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

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongConsumer;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.model.Place;
import com.iris.messages.services.PlatformConstants;
import com.iris.video.VideoRecordingSize;
import com.iris.video.cql.PlaceQuota;
import com.iris.video.service.dao.VideoServiceDao;

public abstract class AbstractVideoQuotaEnforcer implements VideoQuotaEnforcer {
   private static final Logger log = LoggerFactory.getLogger(AbstractVideoQuotaEnforcer.class);

   protected final PlatformMessageBus platformBus;
   protected final VideoServiceDao videoDao;
   protected final boolean defaultDecisionOnFailure;

   protected AbstractVideoQuotaEnforcer(PlatformMessageBus platformBus, VideoServiceDao videoDao, boolean defaultDecisionOnFailure) {
      this.platformBus = platformBus;
      this.videoDao = videoDao;
      this.defaultDecisionOnFailure = defaultDecisionOnFailure;
   }

   @Override
   public boolean allowRecording(Place place, boolean stream, PlaceQuota quota, LongConsumer quotaUpdater) {
      if (stream || quota.isUnderQuota()) {
         return true;
      }

      try {
         // The possible return values are interpreted as follows:
         //    * null  - do not allow the new recording
         //    * empty - allow the new recording without deleting anything
         //    * other - delete the given recordings and allow the new one
         Iterable<VideoRecordingSize> delete = getRecordingsToDelete(place, quota.getUsed(), quota.getQuota(), quotaUpdater);
         if (delete == null) {
            return false;
         }

         long recoveredBytes = 0;
         try {
            for (VideoRecordingSize recording : delete) {
               log.info("marking recording for deletion due to quota enforcement: place={}, recording={}", place.getId(), recording);

               Date scheduledAt = videoDao.deleteRecording(place.getId(), recording.getRecordingId(), recording.isFavorite());
               recoveredBytes += recording.getSize();
               sendDeleteTimeValueChange(place.getId(), place.getPopulation(), recording.getRecordingId(), scheduledAt);
            }
         }
         finally {
            quotaUpdater.accept(recoveredBytes);
         }

         return true;
      } catch (Exception ex) {
         log.info("quota enforcement failed, default decisions {} the new recordings:", defaultDecisionOnFailure ? "allows" : "denies", ex);
         return defaultDecisionOnFailure;
      }
   }

   private void sendDeleteTimeValueChange(UUID placeId, String population, UUID recordingId, Date deleteTime) {
      try {
         Map<String,Object> attrs = ImmutableMap.of(
            RecordingCapability.ATTR_DELETED,true,
            RecordingCapability.ATTR_DELETETIME,deleteTime
         );

         MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attrs);
         PlatformMessage evt = PlatformMessage.buildBroadcast(body, Address.platformService(recordingId, PlatformConstants.SERVICE_VIDEO))
            .withPlaceId(placeId)
            .withPopulation(population)
            .create();

         platformBus.send(evt);
      } catch (Exception ex) {
         log.warn("failed to send value change for recording deleted by quota enforcement");
      }
   }

   @Nullable
   protected abstract Iterable<VideoRecordingSize> getRecordingsToDelete(Place place, long used, long allowed, LongConsumer quotaUpdater) throws Exception;
}

