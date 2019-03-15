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
package com.iris.video.recording;

import static com.iris.video.VideoMetrics.RECORDING_ADDED_FAIL;
import static com.iris.video.VideoMetrics.RECORDING_ADDED_SUCCESS;
import static com.iris.video.VideoMetrics.RECORDING_VC_FAIL;
import static com.iris.video.VideoMetrics.RECORDING_VC_SUCCESS;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.video.VideoMetadata;
import com.iris.video.VideoUtil;

// code formerly in VideoRecordingDao, pulled out to break the tight coupling between video recording dao and event
// messaging to allow different recording server styles to control when they issue events but keep the eventing the
// same
@Singleton
public class RecordingEventPublisher {

   private final PlatformMessageBus bus;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public RecordingEventPublisher(PlatformMessageBus bus, PlacePopulationCacheManager populationCacheMgr) {
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   public void sendAdded(VideoMetadata metadata) {
      if(metadata != null) {
         try {
            Address addr = Address.platformService(metadata.getRecordingId(), RecordingCapability.NAMESPACE);
            MessageBody body = MessageBody.buildMessage(Capability.EVENT_ADDED, metadata.toMap());
            PlatformMessage evt = PlatformMessage.buildBroadcast(body, addr)
               .withActor(VideoUtil.getActorFromPersonId(metadata.getPlaceId(), metadata.getPersonId()))
               .withPlaceId(metadata.getPlaceId())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(metadata.getPlaceId()))
               .create();

            bus.send(evt);
            RECORDING_ADDED_SUCCESS.inc();
         } catch (Exception ex) {
            RECORDING_ADDED_FAIL.inc();
            throw ex;
         }
      }
   }

   public void sendValueChange(UUID placeId, UUID recordingId, double duration, long size, Date purgeAt) {
      Map<String,Object> attrs = ImmutableMap.of(
         RecordingCapability.ATTR_DURATION, duration,
         RecordingCapability.ATTR_SIZE, size,
         RecordingCapability.ATTR_DELETED, true,
         RecordingCapability.ATTR_DELETETIME, purgeAt,
         RecordingCapability.ATTR_COMPLETED, isCompleted(duration)
      );
      sendValueChangeEvent(placeId, recordingId, attrs);
   }

   private boolean isCompleted(double duration) {
      return duration > 0.0;
   }

   public void sendValueChange(UUID placeId, UUID recordingId, double duration, long size) {
      Map<String,Object> attrs = ImmutableMap.of(
         RecordingCapability.ATTR_SIZE, size,
         RecordingCapability.ATTR_DURATION, duration,
         RecordingCapability.ATTR_COMPLETED, isCompleted(duration)
      );
      sendValueChangeEvent(placeId, recordingId, attrs);
   }

   private void sendValueChangeEvent(UUID placeId, UUID recordingId, Map<String, Object> attrs) {
      try {
         Address addr = Address.platformService(recordingId, RecordingCapability.NAMESPACE);

         MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attrs);
         PlatformMessage evt = PlatformMessage.buildBroadcast(body, addr)
            .withPlaceId(placeId)
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
            .create();

         bus.send(evt);
         RECORDING_VC_SUCCESS.inc();
      } catch (Exception ex) {
         RECORDING_VC_FAIL.inc();
         throw ex;
      }
   }

}

