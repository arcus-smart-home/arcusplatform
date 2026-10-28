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

import static org.apache.commons.lang3.StringUtils.trim;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.address.StreetAddressLenientComparator;
import com.iris.platform.location.LocationService;
import com.iris.platform.location.PlaceLocation;

/**
 * Checks an address against the bundled ZIP code table. It is valid when the ZIP code is known and the city and state
 * match the table's. The street lines can't be checked offline.
 *
 * The address as entered is always among the suggestions: a ZIP code can cover several city names, and the table
 * only knows the primary one. When the city or state differs, the table's version comes first.
 */
@Singleton
public class ZipCodeAddressValidator implements AddressValidator {

   private static final StreetAddressLenientComparator COMPARATOR = new StreetAddressLenientComparator();

   private final LocationService locationService;

   @Inject
   public ZipCodeAddressValidator(LocationService locationService) {
      this.locationService = locationService;
   }

   @Override
   public AddressValidationResult validate(StreetAddress address) {
      Optional<PlaceLocation> location = lookup(address.getZip());
      if(!location.isPresent()) {
         return new AddressValidationResult(ImmutableList.of(address), false);
      }

      StreetAddress suggestion = address.copy();
      suggestion.setCity(location.get().getPrimaryCity());
      suggestion.setState(location.get().getState());
      if(COMPARATOR.compare(suggestion, address) == 0) {
         return new AddressValidationResult(ImmutableList.of(address), true);
      }
      return new AddressValidationResult(ImmutableList.of(suggestion, address), false);
   }

   private Optional<PlaceLocation> lookup(String zip) {
      zip = trim(zip);
      if(zip == null || zip.length() < 5) {
         return Optional.empty();
      }
      return locationService.getForZipCode(zip.substring(0, 5));
   }
}
