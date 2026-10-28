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

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.trim;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.location.LocationService;
import com.iris.platform.location.PlaceLocation;

/**
 * Checks an address against the bundled ZIP code table. It is valid when the ZIP code is well formed, is in the table,
 * and is in the state given. The city isn't checked: a ZIP code can cover several city names and the table only has
 * the primary one. The street lines can't be checked offline.
 *
 * When the state is wrong, the only suggestion is the table's city and state for the ZIP code. Otherwise the address
 * is returned as entered, since clients treat an empty suggestion list as a failure.
 */
@Singleton
public class ZipCodeAddressValidator implements AddressValidator {

   // StreetAddress.toPlaceAttributes keeps the first five characters of anything longer as the ZIP code, so anything
   // not in this form would be saved broken.
   private static final Pattern ZIP_PATTERN = Pattern.compile("^(\\d{5})(-\\d{4})?$");

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

      if(equalsIgnoreCase(trim(address.getState()), location.get().getState())) {
         return new AddressValidationResult(ImmutableList.of(address), true);
      }

      StreetAddress suggestion = address.copy();
      suggestion.setCity(location.get().getPrimaryCity());
      suggestion.setState(location.get().getState());
      return new AddressValidationResult(ImmutableList.of(suggestion), false);
   }

   private Optional<PlaceLocation> lookup(String zip) {
      if(zip == null) {
         return Optional.empty();
      }
      Matcher matcher = ZIP_PATTERN.matcher(zip);
      if(!matcher.matches()) {
         return Optional.empty();
      }
      return locationService.getForZipCode(matcher.group(1));
   }
}
