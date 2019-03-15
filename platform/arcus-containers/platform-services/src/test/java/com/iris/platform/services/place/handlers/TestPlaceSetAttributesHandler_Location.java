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

import static java.util.Arrays.asList;
import static java.util.TimeZone.getTimeZone;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.common.sunrise.GeoLocation;
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
import com.iris.platform.address.updater.SmartyStreetsAddressUpdater;
import com.iris.platform.address.validation.smartystreets.DetailedStreetAddress;
import com.iris.platform.address.validation.smartystreets.HttpSmartyStreetsClient;
import com.iris.platform.address.validation.smartystreets.SmartyStreetsClient;
import com.iris.platform.location.LocationService;
import com.iris.platform.location.PlaceLocation;
import com.iris.platform.location.TimezonesModule;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * 
 */
@Mocks({ PlaceDAO.class, LocationService.class, AddressUpdaterFactory.class, HttpSmartyStreetsClient.class})
@Modules({ AttributeMapTransformModule.class, InMemoryMessageModule.class, TimezonesModule.class})
public class TestPlaceSetAttributesHandler_Location extends IrisMockTestCase {
   @Inject PlaceDAO placeDao;
   @Inject LocationService locationService;
   @Inject InMemoryPlatformMessageBus platformBus;
   @Inject SmartyStreetsAddressUpdater smartyStreetsAddressUpdater;
   @Inject AddressUpdaterFactory factory;
   @Inject SmartyStreetsClient smartyStreetsClient;

   @Inject PlaceSetAttributesHandler handler;
   
   Place place;
   Capture<Place> savedPlaces;
   
   @Override
   protected Set<Module> modules()
   {
      Set<Module> modules = super.modules();

      modules.add(new AbstractModule()
      {
         @Override
         protected void configure()
         {
            bind(SmartyStreetsClient.class).to(HttpSmartyStreetsClient.class);
         }
      });

      return modules;
   }
   
   @Before
   public void initPlace() {
      place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      place.setZipCode(null);
      place.setAddrLatitude(0.0);
      place.setAddrLongitude(0.0);
      place.setAddrGeoPrecision(null);
      EasyMock.expect(factory.updaterFor(place)).andReturn(smartyStreetsAddressUpdater).anyTimes();
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
   }

   @Test
   public void testUpdateByZipCode() throws Exception {
      String zipCode = "66047";
      GeoLocation location = GeoLocation.fromCoordinates(-10.0, +20.0);
      PlaceLocation placeLocation = new PlaceLocation(zipCode, location, "Lawrence", "KS", "Douglas", getTimeZone("America/Chicago"));

      DetailedStreetAddress detailedSuggestedAddress = new DetailedStreetAddress();
      detailedSuggestedAddress.setLine1("1651 Naismith Drive");
      detailedSuggestedAddress.setLine2("Suite 1000");
      detailedSuggestedAddress.setCity("Lawrence");
      detailedSuggestedAddress.setState("home");
      detailedSuggestedAddress.setZip("66047");
      detailedSuggestedAddress.setZipPlus4("66047-4069");
      detailedSuggestedAddress.setCountry("US");
      detailedSuggestedAddress.setAddrType("S");
      detailedSuggestedAddress.setAddrZipType("Unique");
      detailedSuggestedAddress.setAddrRDI("Commercial");
      detailedSuggestedAddress.setAddrCountyFIPS("20045");
      EasyMock
         .expect(smartyStreetsClient.getDetailedSuggestions(anyObject()))
         .andReturn(asList(detailedSuggestedAddress));

      EasyMock
         .expect(locationService.getForZipCode(zipCode))
         .andReturn(Optional.of(placeLocation));
      replay();
      
      PlatformMessage message = createSetAttributes(ImmutableMap.of(
            PlaceCapability.ATTR_ZIPCODE, zipCode
      ));
      MessageBody body = handler.handleRequest(place, message);
      
      assertEquals(MessageBody.emptyMessage(), body);
      assertSavedLocationMatches(zipCode, location, Place.GEOPRECISION_ZIP5);
      assertMessageLocationMatches(zipCode, location, Place.GEOPRECISION_ZIP5);
   }

   @Test
   public void testUpdateByZipAndLatLong() throws Exception {
      String zipCode = "66047";
      GeoLocation location = GeoLocation.fromCoordinates(-10.0, +20.0);
      PlaceLocation placeLocation = new PlaceLocation(zipCode, location, "Lawrence", "KS", "Douglas", getTimeZone("America/Chicago"));

      DetailedStreetAddress detailedSuggestedAddress = new DetailedStreetAddress();
      detailedSuggestedAddress.setLine1("1651 Naismith Drive");
      detailedSuggestedAddress.setLine2("Suite 1000");
      detailedSuggestedAddress.setCity("Lawrence");
      detailedSuggestedAddress.setState("home");
      detailedSuggestedAddress.setZip("66047");
      detailedSuggestedAddress.setZipPlus4("66047-4069");
      detailedSuggestedAddress.setCountry("US");
      detailedSuggestedAddress.setAddrType("S");
      detailedSuggestedAddress.setAddrZipType("Unique");
      detailedSuggestedAddress.setAddrRDI("Commercial");
      detailedSuggestedAddress.setAddrCountyFIPS("20045");
      EasyMock
         .expect(smartyStreetsClient.getDetailedSuggestions(anyObject()))
         .andReturn(asList(detailedSuggestedAddress));

      EasyMock
            .expect(locationService.getForZipCode(zipCode))
            .andReturn(Optional.of(placeLocation));
      replay();
      
      PlatformMessage message = createSetAttributes(ImmutableMap.of(
            PlaceCapability.ATTR_ZIPCODE, zipCode,
            PlaceCapability.ATTR_ADDRLATITUDE, location.getLatitude(),
            PlaceCapability.ATTR_ADDRLONGITUDE, location.getLongitude()
      ));
      MessageBody body = handler.handleRequest(place, message);
      
      assertEquals(MessageBody.emptyMessage(), body);
      assertSavedLocationMatches(zipCode, location, Place.GEOPRECISION_UNKNOWN);
      assertMessageLocationMatches(zipCode, location, Place.GEOPRECISION_UNKNOWN);
   }

   @Test
   public void testUpdateByZipLatLongAndSpecifyPrecision() throws Exception {
      String zipCode = "66047";
      GeoLocation location = GeoLocation.fromCoordinates(-10.0, +20.0);
      PlaceLocation placeLocation = new PlaceLocation(zipCode, location, "Lawrence", "KS", "Douglas", getTimeZone("America/Chicago"));

      DetailedStreetAddress detailedSuggestedAddress = new DetailedStreetAddress();
      detailedSuggestedAddress.setLine1("1651 Naismith Drive");
      detailedSuggestedAddress.setLine2("Suite 1000");
      detailedSuggestedAddress.setCity("Lawrence");
      detailedSuggestedAddress.setState("home");
      detailedSuggestedAddress.setZip("66047");
      detailedSuggestedAddress.setZipPlus4("66047-4069");
      detailedSuggestedAddress.setCountry("US");
      detailedSuggestedAddress.setAddrType("S");
      detailedSuggestedAddress.setAddrZipType("Unique");
      detailedSuggestedAddress.setAddrRDI("Commercial");
      detailedSuggestedAddress.setAddrCountyFIPS("20045");
      EasyMock
         .expect(smartyStreetsClient.getDetailedSuggestions(anyObject()))
         .andReturn(asList(detailedSuggestedAddress));

      EasyMock
            .expect(locationService.getForZipCode(zipCode))
            .andReturn(Optional.of(placeLocation));
      replay();
      
      PlatformMessage message = createSetAttributes(ImmutableMap.of(
            PlaceCapability.ATTR_ZIPCODE, zipCode,
            PlaceCapability.ATTR_ADDRLATITUDE, location.getLatitude(),
            PlaceCapability.ATTR_ADDRLONGITUDE, location.getLongitude(),
            PlaceCapability.ATTR_ADDRGEOPRECISION, Place.GEOPRECISION_ZIP9
      ));
      MessageBody body = handler.handleRequest(place, message);
      
      assertEquals(MessageBody.emptyMessage(), body);
      assertSavedLocationMatches(zipCode, location, Place.GEOPRECISION_UNKNOWN);
      assertMessageLocationMatches(zipCode, location, Place.GEOPRECISION_UNKNOWN);
   }

   @Test
   public void testUpdateLatLong() throws Exception {
      GeoLocation location = GeoLocation.fromCoordinates(-10.0, +20.0);
      // no call to location service expected in this case
      replay();
      
      PlatformMessage message = createSetAttributes(ImmutableMap.of(
            PlaceCapability.ATTR_ADDRLATITUDE, location.getLatitude(),
            PlaceCapability.ATTR_ADDRLONGITUDE, location.getLongitude()
      ));
      MessageBody body = handler.handleRequest(place, message);
      
      assertEquals(MessageBody.emptyMessage(), body);
      assertSavedLocationMatches(null, location, Place.GEOPRECISION_UNKNOWN);
      assertMessageLocationMatches(null, location, Place.GEOPRECISION_UNKNOWN);
   }

   private void assertSavedLocationMatches(String zipCode, GeoLocation location, String geoprecision) {
      Place saved = savedPlaces.getValue();
      if(zipCode != null) {
         assertEquals(zipCode, saved.getZipCode());
      }
      if(location != null) {
         assertEquals(location.getLatitude(), place.getAddrLatitude(), 0.1);
         assertEquals(location.getLongitude(), place.getAddrLongitude(), 0.1);
      }
      if(geoprecision != null) {
         assertEquals(geoprecision, place.getAddrGeoPrecision());
      }
   }

   private void assertMessageLocationMatches(String zipCode, GeoLocation location, String geoprecision) throws Exception {
      PlatformMessage response = platformBus.take();
      assertEquals(place.getAddress(), response.getSource().getRepresentation());
      
      MessageBody payload = response.getValue();
      assertEquals(zipCode, PlaceCapability.getZipCode(payload));
      if(location != null) {
         assertEquals(location.getLatitude(), PlaceCapability.getAddrLatitude(payload), .1);
         assertEquals(location.getLongitude(), PlaceCapability.getAddrLongitude(payload), .1);
      }
      assertEquals(geoprecision, PlaceCapability.getAddrGeoPrecision(payload));
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

