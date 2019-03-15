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

import static com.iris.video.recording.RecordingMetrics.RECORDING_STOP_BAD;
import static com.iris.video.recording.RecordingMetrics.RECORDING_STOP_FAIL;
import static com.iris.video.recording.RecordingMetrics.RECORDING_STOP_SUCCESS;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.service.VideoService.StopRecordingRequest;
import com.iris.messages.services.PlatformConstants;
import com.iris.video.VideoSessionRegistry;

import io.netty.channel.ChannelHandlerContext;

@Singleton
public class StopRecordingSnooper extends AbstractPlatformMessageListener {
   private static final Logger log = LoggerFactory.getLogger(StopRecordingSnooper.class);

   private final VideoSessionRegistry registry;

   @Inject
   public StopRecordingSnooper(
         PlatformMessageBus platformBus,
         StopRecordingSnooperConfig config,
         VideoSessionRegistry registry
      ) {
      super(platformBus, "video-stoprecordig-service", config.getMaxThreads(), config.getThreadKeepAliveMs());
      this.registry = registry;
   }

   @Override
   protected void onStart() {
      log.info("Started stop recording snooper service");

      Set<AddressMatcher> matchers = new HashSet<>();
      matchers.add(AddressMatchers.equals(Address.broadcastAddress()));
      matchers.add(AddressMatchers.platformService(MessageConstants.SERVICE, PlatformConstants.SERVICE_VIDEO));

      addListeners(matchers);
   }

   @Override
   protected void onStop() {
      log.info("Shutting down stop recording snooper service");
   }

   @Override
   public void handleMessage(@Nullable PlatformMessage message) {
      if (message == null) {
         return;
      }

      try {
         String type = message.getMessageType();
         switch (type) {
         case StopRecordingRequest.NAME:
            handleStopRecording(message, message.getValue());
            break;
         default:
            // ignore
            break;
         }
      } catch(Exception ex) {
         log.warn("failed to stop recording:", ex);
      }
   }

   protected void handleStopRecording(PlatformMessage message, MessageBody req) throws Exception {
      try {
         String placeId = StopRecordingRequest.getPlaceId(req);
         String recId = StopRecordingRequest.getRecordingId(req);
         if(placeId == null || recId == null) {
            RECORDING_STOP_BAD.inc();
            log.info("snooped request to stop recording with no place id or recording id");
            return;
         }

         ChannelHandlerContext ctx = registry.remove(UUID.fromString(recId));
         if(ctx != null) {
            log.debug("snooper closing channel for recId {}", recId);
            ctx.close();
         }

         RECORDING_STOP_SUCCESS.inc();
      } catch (Throwable th) {
         RECORDING_STOP_FAIL.inc();
         throw th;
      }
   }
}

