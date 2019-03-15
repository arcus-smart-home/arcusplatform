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

import static org.easymock.EasyMock.*;

import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Place;
import com.iris.util.IrisUUID;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by wesleystueve on 6/29/17.
 */
public class TestVideoRecordingFileName {

   @Test
   public void getByDeviceOrDefault() throws ParseException {
      DateFormat utcFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
      utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date date = utcFormat.parse("06/01/2015 11:20:04.453");
      UUID recordingId = IrisUUID.timeUUID(date);
      UUID cameraId = IrisUUID.randomUUID();

      Place place = Fixtures.createPlace();
      place.setTzId("US/Central");

      PlaceDAO placeDAO = createNiceMock(PlaceDAO.class);
      expect(placeDAO.findById(place.getId())).andStubReturn(place);

      Device camera = Fixtures.createDevice();
      camera.setId(cameraId);
      camera.setName("");

      DeviceDAO deviceDAO = createNiceMock(DeviceDAO.class);
      expect(deviceDAO.findById(cameraId)).andStubReturn(camera);

      VideoRecordingFileName sut = new VideoRecordingFileName(
            recordingId,
            cameraId,
            place.getId(),
            deviceDAO,
            placeDAO);

      replay(placeDAO);
      replay(deviceDAO);

      String result = sut.getByDeviceOrDefault();

      Assert.assertEquals(recordingId + ".mp4", result);
   }

   @Test
   public void getByDevice() throws ParseException {
      DateFormat utcFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
      utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date date = utcFormat.parse("06/01/2015 11:20:04.453");
      UUID recordingId = IrisUUID.timeUUID(date);
      UUID cameraId = IrisUUID.randomUUID();

      Place place = Fixtures.createPlace();
      place.setTzId("US/Central");

      PlaceDAO placeDAO = createNiceMock(PlaceDAO.class);
      expect(placeDAO.findById(place.getId())).andStubReturn(place);

      Device camera = Fixtures.createDevice();
      camera.setId(cameraId);
      camera.setName("Cam?Name*With:InvalidChars");

      DeviceDAO deviceDAO = createNiceMock(DeviceDAO.class);
      expect(deviceDAO.findById(cameraId)).andStubReturn(camera);

      VideoRecordingFileName sut = new VideoRecordingFileName(
            recordingId,
            cameraId,
            place.getId(),
            deviceDAO,
            placeDAO);

      replay(placeDAO);
      replay(deviceDAO);

      String result = sut.getByDevice();

      Assert.assertEquals("Cam_Name_With_InvalidChars_20150601_062004.mp4", result);
   }

   @Test
   public void getFileName() {
       VideoRecordingFileName sut = new VideoRecordingFileName(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            createNiceMock(DeviceDAO.class),
            createNiceMock(PlaceDAO.class));

      String result = sut.getFileName("prefix", "suffix");

      Assert.assertEquals("prefix_suffix.mp4", result);
   }

   @Test
   public void getDateSuffix() throws ParseException {
      Place place = Fixtures.createPlace();
      place.setTzId("US/Central");

      DateFormat utcFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
      utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date date = utcFormat.parse("06/01/2015 11:20:04.453");
      UUID recordingId = IrisUUID.timeUUID(date);

      VideoRecordingFileName sut = new VideoRecordingFileName(
            recordingId,
            UUID.randomUUID(),
            place.getId(),
            createNiceMock(DeviceDAO.class),
            createNiceMock(PlaceDAO.class));

      String result = sut.getDateSuffix(place);
      Assert.assertEquals("20150601_062004", result);
   }

   @Test
   public void localDateSuffix() throws ParseException {
      Place place = Fixtures.createPlace();
      place.setTzId("US/Central");

      VideoRecordingFileName sut = new VideoRecordingFileName(
            UUID.randomUUID(),
            UUID.randomUUID(),
            place.getId(),
            createNiceMock(DeviceDAO.class),
            createNiceMock(PlaceDAO.class));

      DateFormat utcFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
      utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

      Date date = utcFormat.parse("06/01/2015 11:20:04.453");

      String result = sut.localDateSuffix(place, date);

      Assert.assertEquals("20150601_062004", result);
   }
}

