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
package com.iris.platform.location;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.common.sunrise.GeoLocation;
import com.iris.messages.model.Place;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({LocationServiceModule.class})
public class TestLocationService extends IrisTestCase {
   
   @Inject private LocationService locationService;
   
   private Place place;

   @Before
   public void setup(){
      place=new Place();
      place.setZipCode("66044");
   }
   
   @Test
   public void testPlaceHasValidZipNoCoordinates(){
      GeoLocation latlong=locationService.getForPlace(place).get().getGeoLocation();
      assertNotNull("Should have found geopoint for zip",latlong);
      assertEquals(38.983551, latlong.getLatitude(), 1.0e-6);
      assertEquals(-95.23202, latlong.getLongitude(), 1.0e-6);
   }
   
   @Test
   public void testPlaceHasValidZipAndCoordinates(){
      GeoLocation location = GeoLocation.fromCoordinates(38.983551, -95.23202);
      place.setAddrLatitude(location.getLatitude());
      place.setAddrLongitude(location.getLongitude());
      GeoLocation latlong=locationService.getForPlace(place).get().getGeoLocation();
      assertNotNull("Should have returned its own coordinates",latlong);
      assertEquals(location.getLatitude(), latlong.getLatitude(), 1.0e-6);
      assertEquals(location.getLongitude(), latlong.getLongitude(), 1.0e-6);
   }
   
   @Test
   public void testPlaceHasNoZipNoCoordinates() {
      place.setZipCode(null);
      assertFalse(locationService.getForPlace(place).isPresent());
   }

   @Test
   public void testPlaceHasInvalidZipNoCoordinates() {
      place.setZipCode("66666");
      assertFalse(locationService.getForPlace(place).isPresent());
   }

}

