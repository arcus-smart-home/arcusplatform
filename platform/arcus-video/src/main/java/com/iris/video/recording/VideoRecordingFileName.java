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

import com.google.common.base.Optional;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Device;
import com.iris.messages.model.Place;
import com.iris.util.IrisFileName;
import com.iris.util.IrisString;
import com.iris.util.IrisUUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by wesleystueve on 6/29/17.
 */
public class VideoRecordingFileName {
   private static final Logger log = LoggerFactory.getLogger(VideoRecordingFileName.class);
   private DeviceDAO deviceDAO;
   private PlaceDAO placeDAO;
   private UUID recordingId;
   private UUID cameraId;
   private UUID placeId;

   public VideoRecordingFileName(
         UUID recordingId,
         UUID cameraId,
         UUID placeId,
         DeviceDAO deviceDAO,
         PlaceDAO placeDAO
         ) {
      this.recordingId = recordingId;
      this.cameraId = cameraId;
      this.placeId = placeId;
      this.deviceDAO = deviceDAO;
      this.placeDAO = placeDAO;
   }

   public String getByDeviceOrDefault() {
      String name = getByDevice();
      if (StringUtils.isEmpty(name)) {
         name = recordingId.toString() + ".mp4";
      }
      return name;
   }

   public String getByDevice() {
      log.trace("Lookup device for camera id [{}]", cameraId);

      Device device = deviceDAO.findById(cameraId);

      if (device != null) {
         log.trace("Found device for camera id [{}]", cameraId);

         //get a clean version of the device name, e.g. replace *? with __
         String cleanDeviceName = IrisFileName.clean(device.getName(), "_");

         if (!StringUtils.isEmpty(cleanDeviceName)) {
            log.trace("Clean device name is [{}]", cleanDeviceName);

            Place place = placeDAO.findById(placeId);
            String dateSuffix = getDateSuffix(place);

            String fileName = getFileName(cleanDeviceName, dateSuffix);
            log.trace("Computed file name [{}] for video recording [{}]", fileName, recordingId);

            if (IrisFileName.isValid(fileName)) {
               log.trace("File name [{}] is valid for video recording [{}]", fileName, recordingId);
               return fileName;
            }
         }
      }

      return "";
   }

   public String getFileName(String prefix, String suffix) {

      String fileName = IrisString.joinIfNotEmpty("_", prefix, suffix);
      if (StringUtils.isEmpty(fileName)) return "";
      return String.format("%s.mp4", fileName);

   }

   public String getDateSuffix(Place place) {

      Date createdDate = new Date(IrisUUID.timeof(recordingId));

      return localDateSuffix(place, createdDate);
   }

   public String localDateSuffix(Place place, Date date) {

      DateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
      Optional<TimeZone> timeZoneOptional = place.getTimeZone();
      TimeZone timeZone = timeZoneOptional.or(TimeZone.getDefault());

      format.setTimeZone(timeZone);

      String retval = format.format(date);
      log.trace("TimeZone [{}] generated [{}] for local date suffix for date [{}] for place [{}].", timeZone.getID(), retval, date, place.getId());

      return retval;
   }


}

