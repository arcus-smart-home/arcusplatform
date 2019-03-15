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
package com.iris.video.service;

import java.util.UUID;

import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.service.VideoService.DeleteAllRequest;
import com.iris.messages.service.VideoService.GetFavoriteQuotaRequest;
import com.iris.messages.service.VideoService.GetQuotaRequest;
import com.iris.messages.service.VideoService.ListRecordingsRequest;
import com.iris.messages.service.VideoService.PageRecordingsRequest;
import com.iris.messages.service.VideoService.RefreshQuotaRequest;
import com.iris.messages.service.VideoService.StartRecordingRequest;
import com.iris.messages.service.VideoService.StopRecordingRequest;

public class VideoServiceUtil {

   public static UUID getPlaceId(PlatformMessage message, MessageBody body) {
      String placeId;
      String attrId;

      switch(message.getMessageType()) {
      case ListRecordingsRequest.NAME:
         placeId = ListRecordingsRequest.getPlaceId(body);
         attrId = ListRecordingsRequest.ATTR_PLACEID;
         break;

      case PageRecordingsRequest.NAME:
          placeId = PageRecordingsRequest.getPlaceId(body);
          attrId = PageRecordingsRequest.ATTR_PLACEID;
          break;

      case StartRecordingRequest.NAME:
         placeId = StartRecordingRequest.getPlaceId(body);
         attrId = StartRecordingRequest.ATTR_PLACEID;
         break;

      case StopRecordingRequest.NAME:
         placeId = StopRecordingRequest.getPlaceId(body);
         attrId = StopRecordingRequest.ATTR_PLACEID;
         break;

      case GetQuotaRequest.NAME:
         placeId = GetQuotaRequest.getPlaceId(body);
         attrId = GetQuotaRequest.ATTR_PLACEID;
         break;
         
      case GetFavoriteQuotaRequest.NAME:
         placeId = GetFavoriteQuotaRequest.getPlaceId(body);
         attrId = GetFavoriteQuotaRequest.ATTR_PLACEID;
         break;    

      case DeleteAllRequest.NAME:
         placeId = DeleteAllRequest.getPlaceId(body);
         attrId = DeleteAllRequest.ATTR_PLACEID;
         break;
      case RefreshQuotaRequest.NAME:
         placeId = RefreshQuotaRequest.getPlaceId(body);
         attrId = RefreshQuotaRequest.ATTR_PLACEID;
         break;
      default:
         return UUID.fromString(message.getPlaceId());
      }

      Errors.assertRequiredParam(placeId, attrId);
      Errors.assertPlaceMatches(message, placeId);
      return UUID.fromString(placeId);
   }

   public static UUID getAccountId(PlatformMessage message, MessageBody body) {
      String accountId = null;
      String attrId = null;

      switch(message.getMessageType()) {
      case StartRecordingRequest.NAME:
         accountId = StartRecordingRequest.getAccountId(body);
         attrId = StartRecordingRequest.ATTR_ACCOUNTID;
         break;
      }

      Errors.assertRequiredParam(accountId, attrId);
      return UUID.fromString(accountId);
   }

   public static UUID getRecordingId(PlatformMessage message, MessageBody body) {
      Object id = message.getDestination().getId();
      if(id == null || !(id instanceof UUID)) {
         throw new NotFoundException(message.getDestination());
      }

      return (UUID)id;
   }

}

