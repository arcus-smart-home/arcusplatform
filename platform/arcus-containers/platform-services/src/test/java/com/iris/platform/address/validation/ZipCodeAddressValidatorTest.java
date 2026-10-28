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
package com.iris.platform.address.validation;

import static java.util.TimeZone.getTimeZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.iris.common.sunrise.GeoLocation;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.location.LocationService;
import com.iris.platform.location.PlaceLocation;

public class ZipCodeAddressValidatorTest {

   private static final PlaceLocation LAWRENCE = new PlaceLocation("66047", GeoLocation.fromCoordinates(38.92, -95.31),
         "Lawrence", "KS", "Douglas", getTimeZone("America/Chicago"));

   private LocationService locationService;
   private ZipCodeAddressValidator validator;

   @Before
   public void setUp() {
      locationService = EasyMock.createMock(LocationService.class);
      validator = new ZipCodeAddressValidator(locationService);
   }

   @After
   public void verify() {
      EasyMock.verify(locationService);
   }

   @Test
   public void testMatchingStateIsValid() {
      expectZip("66047", LAWRENCE);
      StreetAddress address = address("lawrence", "ks", "66047");

      AddressValidationResult result = validator.validate(address);

      assertTrue(result.isValid());
      assertEquals(ImmutableList.of(address), result.getSuggestions());
   }

   @Test
   public void testStateIsTrimmed() {
      expectZip("66047", LAWRENCE);
      StreetAddress address = address("Lawrence", " KS ", "66047");

      AddressValidationResult result = validator.validate(address);

      assertTrue(result.isValid());
      assertEquals(ImmutableList.of(address), result.getSuggestions());
   }

   // A ZIP code can cover several city names and the table only has the primary one.
   @Test
   public void testOtherCityNameIsValid() {
      expectZip("66047", LAWRENCE);
      StreetAddress address = address("Eudora", "KS", "66047");

      AddressValidationResult result = validator.validate(address);

      assertTrue(result.isValid());
      assertEquals(ImmutableList.of(address), result.getSuggestions());
   }

   @Test
   public void testZipPlus4IsLookedUpByZip5() {
      expectZip("66047", LAWRENCE);
      StreetAddress address = address("Lawrence", "KS", "66047-4069");

      AddressValidationResult result = validator.validate(address);

      assertTrue(result.isValid());
      assertEquals(ImmutableList.of(address), result.getSuggestions());
   }

   @Test
   public void testWrongStateSuggestsTableCityAndState() {
      expectZip("66047", LAWRENCE);
      StreetAddress address = address("Kansas City", "MO", "66047");

      AddressValidationResult result = validator.validate(address);

      assertFalse(result.isValid());
      assertEquals(ImmutableList.of(address("Lawrence", "KS", "66047")), result.getSuggestions());
   }

   @Test
   public void testMissingStateSuggestsTableCityAndState() {
      expectZip("66047", LAWRENCE);
      StreetAddress address = address("Lawrence", null, "66047");

      AddressValidationResult result = validator.validate(address);

      assertFalse(result.isValid());
      assertEquals(ImmutableList.of(address("Lawrence", "KS", "66047")), result.getSuggestions());
   }

   // Clients treat an empty suggestion list as a failure, so an unknown ZIP code still offers the address as entered.
   @Test
   public void testUnknownZipOffersEnteredAddress() {
      expectZip("00000", null);
      StreetAddress address = address("Lawrence", "KS", "00000");

      AddressValidationResult result = validator.validate(address);

      assertFalse(result.isValid());
      assertEquals(ImmutableList.of(address), result.getSuggestions());
   }

   // These would be saved as a broken ZIP code, so none of them may pass on the strength of their first five digits.
   @Test
   public void testMalformedZipIsNotLookedUp() {
      EasyMock.replay(locationService);
      for(String zip : Arrays.asList(null, "", "660", "660471", "66047abc", " 66047", "66047 ", "66047-40", "660474069")) {
         StreetAddress address = address("Lawrence", "KS", zip);

         AddressValidationResult result = validator.validate(address);

         assertFalse(zip, result.isValid());
         assertEquals(zip, ImmutableList.of(address), result.getSuggestions());
      }
   }

   private void expectZip(String zip, PlaceLocation location) {
      EasyMock.expect(locationService.getForZipCode(zip)).andReturn(Optional.ofNullable(location));
      EasyMock.replay(locationService);
   }

   private static StreetAddress address(String city, String state, String zip) {
      StreetAddress address = new StreetAddress();
      address.setLine1("1651 Naismith Drive");
      address.setCity(city);
      address.setState(state);
      address.setZip(zip);
      return address;
   }
}
