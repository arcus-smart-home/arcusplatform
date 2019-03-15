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
/**
 * 
 */
package com.iris.platform.services.place.handlers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Place;
import com.iris.platform.address.updater.AddressUpdaterFactory;
import com.iris.platform.location.LocationService;
import com.iris.platform.location.TimezonesModule;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * 
 */
@Mocks({ PlaceDAO.class, LocationService.class, AddressUpdaterFactory.class})
@Modules({ AttributeMapTransformModule.class, InMemoryMessageModule.class, TimezonesModule.class })
@RunWith(Parameterized.class)
public class TestPlaceSetAttributesHandler extends IrisMockTestCase {
   @Inject PlaceDAO placeDao;
   @Inject InMemoryPlatformMessageBus platformBus;
   
   @Inject PlaceSetAttributesHandler handler;
   
   Place place;
   Capture<Place> savedPlaces;
   
   @Parameters(name="{0} - {1}")
   public static List<Object[]> parameters() {
      return Arrays.asList(
    		  //{timeZoneId, expectedTimezoneName, expectedTimezoneOffset, expectedUseDST}
            new Object[] { null, "Atlantic Standard Time", -4.0, true },
            new Object[] { "Etc/GMT+4", "Etc/GMT+4", -4.0, false },
            new Object[] { "US/Eastern", "US/Eastern", -5.0, true },
            new Object[] { "US/Central", "US/Central", -6.0, true },
            new Object[] { "US/Mountain", "US/Mountain", -7.0, true },
            new Object[] { "US/Arizona", "US/Arizona", -7.0, false },
            new Object[] { "US/Pacific", "US/Pacific", -8.0, true },
            new Object[] { "US/Alaska", "US/Alaska", -9.0, true },
            // FIXME these don't work on the build server
//            new Object[] { "US/Aleutian", "Aleutian", "Hawaii-Aleutian Standard Time", -10.0, true },
//            new Object[] { "US/Aleutian", "Hawaii", "Hawaii-Aleutian Standard Time", -10.0, true },
            new Object[] { "US/Hawaii", "US/Hawaii", -10.0, false },
            new Object[] { "US/Samoa", "US/Samoa", -11.0, false },
            new Object[] { "Etc/GMT-12", "Etc/GMT-12", 12.0, false },
            new Object[] { "Etc/GMT-11", "Etc/GMT-11", 11.0, false },
            new Object[] { "Etc/GMT-10", "Etc/GMT-10", 10.0, false },
            new Object[] { "Etc/GMT-9", "Etc/GMT-9", 9.0, false },
            new Object[] { "America/New_York", "Eastern", -5.0, true },
            new Object[] { "America/Los_Angeles", "Pacific", -8.0, true },
            new Object[] { "America/Indiana/Indianapolis", "Indiana/Indianapolis", -5.0, true },
            new Object[] { "America/North_Dakota/New_Salem", "North Dakota/New Salem", -6.0, true },
            new Object[] { "Pacific/Pago_Pago", "Samoa", -11.0, false }
            
      );
   }
   
   String timeZoneId = "US/Eastern";
   String expectedTimezoneName = "Eastern";
   double expectedTimezoneOffset = -5.0;
   boolean expectedUseDST = true;
   
   public TestPlaceSetAttributesHandler(
         String timeZoneId,
         String expectedTimezoneName,
         double timeZoneOffset,
         boolean usesDst
   ) {
      this.timeZoneId = timeZoneId;
      this.expectedTimezoneName = expectedTimezoneName;
      this.expectedTimezoneOffset = timeZoneOffset;
      this.expectedUseDST = usesDst;
   }
   
   @Before
   public void initPlace() {
      place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      place.setTzId(null);
      place.setTzName(null);
      place.setTzOffset(null);
      place.setTzUsesDST(null);
   }
   
   @Before
   public void captureSaves() {
      savedPlaces = Capture.newInstance(CaptureType.ALL);
      EasyMock
         .expect(placeDao.save(EasyMock.capture(savedPlaces)))
         .andAnswer(() -> {
            Place p = savedPlaces.getValue().copy();
            p.setModified(new Date());
            return p;
         })
         .anyTimes();
      replay();
   }

   @Test
   public void testUpdateByTimezoneId() throws Exception {
      if(timeZoneId == null) {
         return;
      }
      
      PlatformMessage message = createSetAttributes(ImmutableMap.of(PlaceCapability.ATTR_TZID, timeZoneId));
      MessageBody body = handler.handleRequest(place, message);
      
      assertEquals(MessageBody.emptyMessage(), body);
      assertSavedPlaceMatches(expectedTimezoneName);
      assertPlatformMessageMatches(expectedTimezoneName);
   }

   private void assertSavedPlaceMatches(String timeZoneName) {
      Place saved = savedPlaces.getValue();
      if(timeZoneId != null) assertEquals(timeZoneId, saved.getTzId());
      assertEquals(timeZoneName, saved.getTzName());
      assertEquals(expectedTimezoneOffset, saved.getTzOffset(), 0.01);
      assertEquals(expectedUseDST, saved.getTzUsesDST());
   }

   private void assertPlatformMessageMatches(String timeZoneName) throws Exception {
      PlatformMessage response = platformBus.take();
      assertEquals(place.getAddress(), response.getSource().getRepresentation());
      
      MessageBody payload = response.getValue();
      if(timeZoneId != null) assertEquals(timeZoneId, PlaceCapability.getTzId(payload));
      assertEquals(timeZoneName, PlaceCapability.getTzName(payload));
      assertEquals(expectedTimezoneOffset, PlaceCapability.getTzOffset(payload), 0.01);
      assertEquals(expectedUseDST, PlaceCapability.getTzUsesDST(payload));
   }

   private PlatformMessage createSetAttributes(ImmutableMap<String, Object> attributes) {
      return
            PlatformMessage
               .request(Address.fromString(place.getAddress()))
               .from(Fixtures.createClientAddress())
               .withPayload(Capability.EVENT_SET_ATTRIBUTES_ERROR, attributes)
               .create();
   }
}

